# GrantTrack — Funding Module Deep-Dive (Interview Study Guide)

Everything about the Funding module end-to-end: the four entities and their forms (**Sponsors,
Institutions, Funding Schemes, Grant Calls**), how the forms are built with **reactive forms**, how a
dropdown like "Sponsor" **auto-loads its options**, how choosing **"Others"** reveals a free-text
input, how an **uploaded file is stored** and where the path comes from, how the **grant-call
lifecycle / Open action** works and how an **open call reaches every researcher**, and how the
human-readable **IDs (SCH00001, INST00001, SP000001)** are generated — all with exact file locations
and the real code.

> Funding is **not** a microservice — it lives in the monolithic **core-service**
> (`Backend/core-service/.../funding`), reached through the API gateway. The frontend lives in
> `Frontend/src/app/features/funding`. (See the Auth guide for how the token/gateway/JWT plumbing
> works — it's identical here.)

---

## 0. The 10-second summary

> "Funding has four resources — Sponsors, Institutions, Funding Schemes and Grant Calls — each with a
> list page + a create/edit modal built with **Angular reactive forms**. A Scheme belongs to a
> Sponsor, a Grant Call belongs to a Scheme, so their forms **auto-load** the parent list into a
> `<select>`. Some fields offer **'Others'** which reveals a free-text input. A scheme can carry an
> uploaded document, stored on disk under a configured `upload-dir` with the relative path saved on
> the row. Each resource gets a readable **code** (SCH/INST/SP + zero-padded id) generated on the
> server. The whole workflow starts here: an admin opens a **Grant Call**, and every researcher then
> sees it under **Open Calls** to apply."

---

## 1. Files map

| Concern | Frontend | Backend (core-service) |
|---|---|---|
| Sponsors | `features/funding/sponsors/sponsors-list.component.ts` | `funding/controller/SponsorController.java`, `service/impl/SponsorServiceImpl.java`, `entity/Sponsor.java` |
| Institutions | `features/funding/institutions/institutions-list.component.ts` | `InstitutionController.java`, `InstitutionServiceImpl.java`, `entity/Institution.java` |
| Funding Schemes | `features/funding/schemes/schemes-list.component.ts` (+ `.html`) | `FundingSchemeController.java`, `FundingSchemeServiceImpl.java`, `entity/FundingScheme.java` |
| Grant Calls | `features/funding/calls/calls-list.component.ts` (+ `.html`) | `GrantCallController.java`, `GrantCallServiceImpl.java`, `entity/GrantCall.java` |
| Shared API client | [`features/funding/funding.service.ts`](Frontend/src/app/features/funding/funding.service.ts) | — |
| Mapping to DTOs | — | [`funding/mapper/FundingMapper.java`](Backend/core-service/src/main/java/com/granttrack/funding/mapper/FundingMapper.java) |
| File storage | (upload in each form) | [`application/service/DocumentStorageService.java`](Backend/core-service/src/main/java/com/granttrack/application/service/DocumentStorageService.java) |
| Researcher "Open Calls" | [`features/applications/opportunities/opportunities.component.ts`](Frontend/src/app/features/applications/opportunities/opportunities.component.ts) | (reads calls with status OPEN) |

**Relationships:** `Sponsor 1—* FundingScheme 1—* GrantCall`. Institutions are standalone reference
data. All funding endpoints are under `/api/v1/funding/**` and route to core-service via the gateway.

---

## 2. How the forms are built — Angular reactive forms

All four forms use the same recipe: `FormBuilder.nonNullable.group({...})` with `Validators`, a single
form reused for **create and edit** (distinguished by an `editingId` signal), and a `save()` that
branches to create vs update.

**Example — the Scheme form** ([`schemes-list.component.ts`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts)):
```ts
readonly form = this.fb.nonNullable.group({
  schemeName:        ['', [Validators.required, Validators.maxLength(200)]],
  sponsorId:         [null as number | null, [Validators.required]],   // parent dropdown
  researchArea:      ['', [Validators.required, Validators.maxLength(200)]],
  researchAreaOther: [''],                                             // shown only when "Others"
  category:          ['', [Validators.required, Validators.maxLength(100)]],
  categoryOther:     [''],
  minAwardAmount:    [null as number | null, [Validators.required, Validators.min(0)]],
  maxAwardAmount:    [null as number | null, [Validators.required, Validators.min(1)]],
  eligibleApplicants:['', [Validators.required, Validators.maxLength(500)]],
  eligibleApplicantsOther: [''],
  fromDate: [''], toDate: [''], fundingDurationMonths: [null as number | null],
  description: ['', [Validators.required]],
  status: ['ACTIVE', [Validators.required]],
});
```
- The template binds it with `[formGroup]="form"` and each control with `formControlName="..."`
  ([`schemes-list.component.html`](Frontend/src/app/features/funding/schemes/schemes-list.component.html)), e.g.:
  ```html
  <form [formGroup]="form" id="schemeForm" (ngSubmit)="save()">
    <input class="form-control" formControlName="schemeName" [class.is-invalid]="invalid('schemeName')" />
    @if (invalid('schemeName')) { <div class="text-danger small">Scheme name is required.</div> }
  ```
- Field-level error display uses a helper:
  ```ts
  invalid(ctrl: string): boolean { const c = this.form.get(ctrl); return !!c && c.invalid && c.touched; }
  ```
- **One form, create or edit:** `openCreate()` sets `editingId = null` and resets the form;
  `openEdit(s)` sets `editingId = s.id` and patches the row's values. `save()` branches:
  ```ts
  const id = this.editingId();
  const req = id ? this.api.updateScheme(id, body) : this.api.createScheme(body);
  ```
- **Validation happens on both ends:** client-side `Validators` here; server-side Bean Validation on
  the request record (e.g. `SponsorRequest` has `@NotBlank`, `@Email`, `@Pattern(\d{10})` for phone).

**Talking point:** reactive forms keep validation + state in TypeScript (testable, typed), and the
same `form` object drives create and edit — only `editingId` and the submit branch differ.

---

## 2A. How the Edit (Update) operation works — full flow

Edit reuses **the same modal, the same `form`, and the same `save()`** as create. The only thing that
distinguishes them is one signal, `editingId`: `null` ⇒ create, a number ⇒ edit. This is worth
explaining slowly in an interview because it touches four things: a **pre-fill** step, the
**"Others" reverse-mapping**, the **create-vs-update branch**, and the **backend PUT** (which behaves
differently from create).

### Step 1 — the "Edit" action opens the modal pre-filled
The row's action menu calls `openEdit(row)`. Compare it with `openCreate()`
([`schemes-list.component.ts`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts)):

```ts
readonly editingId = signal<number | null>(null);
// modal title flips automatically based on the same signal:
readonly modalTitle = computed(() => (this.editingId() ? 'Edit Funding Scheme' : 'Create Funding Scheme'));

openCreate(): void {
  this.editingId.set(null);          // ← CREATE mode
  this.selectedFile.set(null);
  this.form.reset({ /* blank defaults, status:'ACTIVE', fromDate: today … */ });
  this.modalOpen.set(true);
}

openEdit(s: FundingSchemeResponse): void {
  this.editingId.set(s.id);          // ← EDIT mode (remembers WHICH row)
  this.selectedFile.set(null);       // don't carry a stale file selection
  // …reverse-map "Others" (Step 2)…
  this.form.reset({ /* patched with the row's current values */ });
  this.modalOpen.set(true);
}
```
Key points: `editingId` both **remembers which id to PUT** and **drives the modal title** (via the
`computed`). `openEdit` uses `form.reset({...})` (not `patchValue`) so every control — including ones
the row doesn't set — starts from a known state.

### Step 2 — reverse-mapping "Others" when pre-filling
On create, `save()` collapses `"Others"` into the typed value, so the DB stores the real string
(e.g. `"Nanotechnology"`). On edit we must do the **reverse**: if the stored value isn't one of the
fixed options, it must have been an "Others" entry, so re-select `"Others"` and drop the value back
into the `*Other` input ([`openEdit`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts)):
```ts
let cat = s.category ?? '', catOther = '';
if (cat && !this.categories.includes(cat)) { catOther = cat; cat = 'Others'; }   // "Nanotech" → select Others + fill categoryOther
// (same for researchArea and eligibleApplicants)
this.form.reset({
  schemeName: s.schemeName,
  sponsorId:  s.sponsorId,                         // pre-selects the right <option [ngValue]="s.id">
  category: cat, categoryOther: catOther,
  researchArea: area, researchAreaOther: areaOther,
  eligibleApplicants: app, eligibleApplicantsOther: appOther,
  minAwardAmount: s.minAwardAmount, maxAwardAmount: s.maxAwardAmount,
  fromDate: s.fromDate ?? '', toDate: s.toDate ?? '',
  description: s.description ?? '', fundingDurationMonths: s.fundingDurationMonths ?? null,
  status: s.status,
});
```
Because the sponsor dropdown was already auto-loaded on `ngOnInit` (see §3), setting `sponsorId` to the
row's value makes the correct sponsor appear selected — `[ngValue]="s.id"` matches by id.

### Step 3 — one `save()` branches create vs update
The submit is shared; the id decides which HTTP verb runs
([`save()`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts)):
```ts
const v = this.form.getRawValue();
const body = { /* same body-building + "Others"→typed-value collapse as create */ };
this.saving.set(true);
const id = this.editingId();
const req = id ? this.api.updateScheme(id, body)   // ← PUT /funding/schemes/{id}
              : this.api.createScheme(body);        // ← POST /funding/schemes
req.subscribe({
  next: (res) => {
    const file = this.selectedFile();
    if (file) {                                      // optional: only if a NEW file was chosen
      this.api.uploadSchemeDocument(res.data.id, file).subscribe({ next: () => this.finishSave(), ... });
    } else { this.toast.success(id ? 'Scheme updated.' : 'Scheme created.'); this.finishSave(); }
  },
  error: () => this.saving.set(false),
});
```
`updateScheme` is just a PUT in [`funding.service.ts`](Frontend/src/app/features/funding/funding.service.ts):
```ts
updateScheme(id: number, body: FundingSchemeRequest) {
  return this.http.put<ApiResponse<FundingSchemeResponse>>(`${this.base}/schemes/${id}`, body);
}
```
Note the **document on edit is optional**: the file input starts empty (`selectedFile = null`), so if
the user doesn't pick a new file the existing `documentPath` is left untouched; picking one re-uploads
and overwrites via the same `/schemes/{id}/document` call.

### Step 4 — the backend PUT (different from create in three ways)
[`FundingSchemeServiceImpl.update`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/FundingSchemeServiceImpl.java):
```java
@Override @Transactional
public FundingSchemeResponse update(Long id, FundingSchemeRequest request) {
    validateAwardRange(request);                          // same business validation as create
    FundingScheme scheme = find(id);                      // 404 if id unknown (ResourceNotFoundException)
    if (!scheme.getSponsor().getId().equals(request.sponsorId())) {   // only re-fetch sponsor if it CHANGED
        Sponsor sponsor = sponsorRepository.findById(request.sponsorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", request.sponsorId()));
        scheme.setSponsor(sponsor);
    }
    scheme.setSchemeName(request.schemeName());
    scheme.setResearchArea(request.researchArea());
    scheme.setCategory(request.category());
    scheme.setMaxAwardAmount(request.maxAwardAmount());
    scheme.setMinAwardAmount(request.minAwardAmount());
    scheme.setEligibleApplicants(request.eligibleApplicants());
    scheme.setFundingDurationMonths(computeDuration(request));
    scheme.setFromDate(request.fromDate());
    scheme.setToDate(request.toDate());
    scheme.setDescription(request.description());
    if (StringUtils.hasText(request.status())) {          // status only changes if one was sent
        scheme.setStatus(parseStatus(request.status(), scheme.getStatus()));
    }
    return mapper.toResponse(schemeRepository.save(scheme));   // ONE save — no code regeneration
}
```
Three differences from `create()` — the exact things to call out:
1. **It loads the existing managed entity first (`find(id)`)** and mutates its fields, rather than
   `builder()`-ing a fresh row. Because the entity is JPA-managed inside `@Transactional`, `save()` is
   really a dirty-check UPDATE, not an INSERT.
2. **The `schemeCode` is never touched.** The `SCH00001` code is generated once on create (from the
   id) and stays stable across edits — so update does a **single** `save()`, not the create's
   save-then-set-code-then-save.
3. **Foreign-key change is guarded.** The sponsor is only re-fetched from the DB when
   `request.sponsorId()` differs from the current one — avoids a needless query and a bogus 404 when
   the sponsor is unchanged.

Business rules are re-checked on every edit (`validateAwardRange` → min ≤ max, toDate ≥ fromDate) and
`@PreAuthorize` on the controller still restricts who may update. Sponsor/Institution/Grant-Call all
follow the identical `update(id, request)` shape (load → mutate → save; code untouched).

**Interview line:** *"Edit and create share one modal, one form, and one `save()` — an `editingId`
signal is the switch. On open we pre-fill the form and reverse-map 'Others' values; on submit the id
picks PUT vs POST. Server-side, update loads the managed entity and mutates it (a dirty-check UPDATE),
never regenerates the code, and only re-fetches the sponsor if the FK actually changed."*

---

## 3. How a dropdown auto-renders its options (e.g. Sponsor in the Scheme form)

A Scheme belongs to a Sponsor, so the Scheme form loads the sponsor list into memory on init and
renders it as `<option>`s.

**Load on init** — [`schemes-list.component.ts`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts):
```ts
readonly sponsors = signal<SponsorResponse[]>([]);

ngOnInit(): void {
  this.load();                                                       // the schemes table
  this.api.listSponsors({ size: 200 }).subscribe((r) => this.sponsors.set(r.data.content));  // ← fetch sponsors once
}
```
**Render as a select** — [`schemes-list.component.html`](Frontend/src/app/features/funding/schemes/schemes-list.component.html):
```html
<select class="form-select" formControlName="sponsorId" [class.is-invalid]="invalid('sponsorId')">
  <option [ngValue]="null" disabled>Select sponsor…</option>
  @for (s of sponsors(); track s.id) {
    <option [ngValue]="s.id">{{ s.name }}</option>                   <!-- value = id, label = name -->
  }
</select>
```
`this.api.listSponsors(...)` → [`funding.service.ts`](Frontend/src/app/features/funding/funding.service.ts) → `GET /api/v1/funding/sponsors` (through the gateway,
JWT attached by the interceptor). The response fills the `sponsors` signal; `@for` renders one option
per sponsor; the selected `sponsorId` is what the backend stores as the scheme's FK.

> Same pattern elsewhere: the **Grant Call** form loads **ACTIVE schemes** into its scheme dropdown —
> [`calls-list.component.ts`](Frontend/src/app/features/funding/calls/calls-list.component.ts): `this.api.listSchemes({ size: 200, status: 'ACTIVE' })`.

**Talking point:** the parent list is fetched once on `ngOnInit` into a **signal**; `@for` over the
signal renders the options, and `formControlName` binds the chosen id straight into the reactive form.

---

## 4. The "Others" option — revealing a free-text input

Some selects (category, research area, eligible applicants) include an `'Others'` choice. When it's
selected, a text input appears; on save the typed value replaces `'Others'`.

**The fixed option lists** ([`schemes-list.component.ts`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts)):
```ts
readonly categories    = ['Basic Research', 'Applied Research', 'Translational', 'Development', 'Others'];
readonly researchAreas = ['Engineering', 'Medical', 'Physical Sciences', 'Social Sciences', 'Others'];
readonly applicantTypes = ['M.E.', 'M.Tech', 'M.Sc.', 'Ph.D.', 'Others'];
```
**Conditionally show the input** ([`schemes-list.component.html`](Frontend/src/app/features/funding/schemes/schemes-list.component.html)):
```html
<select class="form-select" formControlName="category">
  <option value="" disabled>Select category…</option>
  @for (c of categories; track c) { <option [value]="c">{{ c }}</option> }
</select>
@if (form.get('category')?.value === 'Others') {                     <!-- appears only when "Others" chosen -->
  <input class="form-control mt-2" formControlName="categoryOther" placeholder="Specify category" />
}
```
`@if` re-evaluates whenever the control value changes, so picking "Others" instantly reveals
`categoryOther`; picking anything else hides it.

**Collapse "Others" → the typed value on save** ([`schemes-list.component.ts`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts) `save()`):
```ts
const body = {
  ...
  category:      v.category      === 'Others' ? v.categoryOther      : v.category,
  researchArea:  v.researchArea  === 'Others' ? v.researchAreaOther  : v.researchArea,
  eligibleApplicants: v.eligibleApplicants === 'Others' ? v.eligibleApplicantsOther : v.eligibleApplicants,
  ...
};
```
So the backend only ever stores the real value (e.g. `"Nanotechnology"`), never the literal
`"Others"`.

**The reverse on edit** ([`openEdit`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts)) — if a stored value isn't in the fixed list, it must have been an
"Others" entry, so the form re-selects "Others" and puts the value into the *Other input:
```ts
let cat = s.category ?? '', catOther = '';
if (cat && !this.categories.includes(cat)) { catOther = cat; cat = 'Others'; }   // stored "Nanotech" → select "Others" + fill categoryOther
this.form.reset({ ..., category: cat, categoryOther: catOther, ... });
```

**Talking point:** the "Others" pattern is pure template logic (`@if` on the control value) plus a
save/edit transform — no backend involvement; the server sees a normal string.

---

## 5. How an uploaded file is stored, and where the path comes from

A Funding Scheme can carry a supporting document. The upload is a **second call after the scheme is
created** (the file needs the scheme id), and the file lands on disk under a configured directory
while only the **relative path** is stored on the row.

### Frontend — pick the file, then upload after create
[`schemes-list.component.ts`](Frontend/src/app/features/funding/schemes/schemes-list.component.ts):
```ts
readonly selectedFile = signal<File | null>(null);
onFileSelected(event: any): void { const f = event.target.files[0]; if (f) this.selectedFile.set(f); }

// inside save(), after createScheme/updateScheme returns the scheme:
req.subscribe({
  next: (res) => {
    const file = this.selectedFile();
    if (file) {
      this.api.uploadSchemeDocument(res.data.id, file).subscribe({ next: () => { ...; this.finishSave(); } });
    } else { ...; this.finishSave(); }
  },
});
```
[`funding.service.ts`](Frontend/src/app/features/funding/funding.service.ts) sends it as `multipart/form-data`:
```ts
uploadSchemeDocument(id: number, file: File): Observable<ApiResponse<FundingSchemeResponse>> {
  const formData = new FormData();
  formData.append('file', file);
  return this.http.post(`${this.base}/schemes/${id}/document`, formData);   // POST /api/v1/funding/schemes/{id}/document
}
```

### Backend — store on disk, persist the path
Controller: `POST /schemes/{id}/document` (multipart) → service:
[`FundingSchemeServiceImpl.uploadDocument`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/FundingSchemeServiceImpl.java)
```java
public FundingSchemeResponse uploadDocument(Long id, MultipartFile file) {
    FundingScheme scheme = find(id);
    String relativePath = documentStorageService.storeSchemeDocument(id, file);  // writes the file
    scheme.setDocumentPath(relativePath);                                         // persist ONLY the path
    return mapper.toResponse(schemeRepository.save(scheme));
}
```
[`DocumentStorageService`](Backend/core-service/src/main/java/com/granttrack/application/service/DocumentStorageService.java) does the actual disk write:
```java
public DocumentStorageService(@Value("${granttrack.storage.upload-dir:./uploads}") String uploadDir) {
    this.root = Paths.get(uploadDir).toAbsolutePath().normalize();     // ← WHERE the path is rooted
}
public String storeSchemeDocument(Long schemeId, MultipartFile file) {
    return store(file, "schemes/" + schemeId, Set.of("pdf","doc","docx","jpg","jpeg","png"), MAX_BYTES);
}
private String store(MultipartFile file, String relativeDir, Set<String> allowed, long maxSize) {
    // validate size + extension...
    Path dir = root.resolve(relativeDir); Files.createDirectories(dir);          // <root>/schemes/<id>/
    String stored = UUID.randomUUID() + "." + ext;                              // random filename (no clashes)
    Files.copy(file.getInputStream(), dir.resolve(stored), REPLACE_EXISTING);
    return relativeDir + "/" + stored;                                          // e.g. "schemes/12/ab34….pdf" → saved on the row
}
```

### Where the path is configured
- Property `granttrack.storage.upload-dir` — [`core-service/.../resources/application.yml`](Backend/core-service/src/main/resources/application.yml)
  (`${GRANTTRACK_UPLOAD_DIR:./uploads}`), pinned to an **absolute** path in
  [`application-local.yml`](Backend/core-service/src/main/resources/application-local.yml) so storage is independent of the working directory:
  ```yaml
  granttrack:
    storage:
      upload-dir: C:/Users/.../GT_project/Backend/uploads
  ```
- So the file physically lands at `<upload-dir>/schemes/<schemeId>/<uuid>.pdf`, and the DB stores the
  **relative** `documentPath` (`schemes/<id>/<uuid>.pdf`). Download resolves `root.resolve(path)` and
  streams it back (with a `..`/path-traversal guard).

**Talking points:** (1) the file is saved on disk, only the *path* in the DB (keeps the DB small);
(2) the storage root is **externalised config**, so prod can point at a mounted volume/bucket dir;
(3) filenames are UUIDs so two uploads never collide; (4) extension + size are validated server-side.

---

## 6. Grant-call lifecycle — the "Open" action and how a call reaches every researcher

### The lifecycle (state machine)
```
UPCOMING ──open()──► OPEN ──close()──► CLOSED       (also → UNDER_REVIEW → AWARDED during review/award)
   └────────────── terminate() ──► TERMINATED
```
A call is **created as UPCOMING**; an admin then **opens** it.

### The "Open" action shows only for UPCOMING calls
[`calls-list.component.html`](Frontend/src/app/features/funding/calls/calls-list.component.html) (row actions dropdown):
```html
@if (row.status === 'UPCOMING') {
  <button class="dropdown-item" (click)="openCall(row)"><gt-icon name="check-circle" /> Open</button>
  ...
}
```
[`calls-list.component.ts`](Frontend/src/app/features/funding/calls/calls-list.component.ts):
```ts
openCall(c): void {
  this.api.openCall(c.id).subscribe(() => { this.toast.success('Call opened for submissions.'); this.load(); });
}
```
`api.openCall` → `POST /api/v1/funding/calls/{id}/open` → controller
(`@PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")`) → service.

### Backend guards the transition — [`GrantCallServiceImpl.open`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/GrantCallServiceImpl.java)
```java
public GrantCallResponse open(Long id) {
    GrantCall call = find(id);
    if (call.getStatus() != CallStatus.UPCOMING) throw new BusinessException("Only an UPCOMING call can be opened …");
    if (call.getScheme().getStatus() != SchemeStatus.ACTIVE) throw new BusinessException("… scheme is not ACTIVE");
    if (call.getCloseDate().isBefore(LocalDate.now())) throw new BusinessException("… close date has already passed");
    call.setStatus(CallStatus.OPEN);
    return mapper.toResponse(callRepository.save(call));
}
```

### How an OPEN call reaches EVERY researcher (no per-user feed — just a status query)
The researcher's **Open Calls** page fetches calls whose **status = OPEN**:
[`opportunities.component.ts`](Frontend/src/app/features/applications/opportunities/opportunities.component.ts)
```ts
forkJoin({
  calls:   this.funding.listCalls({ status: 'OPEN', size: 100, sort: 'closeDate,asc' }),   // ← only OPEN calls
  schemes: this.funding.listSchemes({ status: 'ACTIVE', size: 200 }),
})...
apply(c: GrantCallResponse): void { /* launches the application wizard for this call */ }
```
`listCalls({status:'OPEN'})` → `GET /api/v1/funding/calls?status=OPEN` → [`GrantCallServiceImpl.search`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/GrantCallServiceImpl.java)
builds a JPA Specification `where status = OPEN`. So the **moment** an admin flips a call to OPEN, it
appears in every researcher's Open Calls list (and in the application wizard's call picker, which also
loads `status:'OPEN'`). Clicking **Apply** starts an application against that call.

**Interview line:** *"There's no special 'push to researchers'. Researchers simply query calls with
status OPEN; opening a call is a one-field status change, and it's instantly visible to everyone
because they all read the same OPEN filter."*

---

## 7. How the readable IDs are generated (SCH00001, INST00001, SP000001)

Each resource stores a unique, human-friendly **code** alongside its numeric primary key. The code is
generated **on the server**, right after the row is saved (so the DB-assigned id exists), then saved
again.

- **Scheme** — [`FundingSchemeServiceImpl.create`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/FundingSchemeServiceImpl.java):
  ```java
  scheme = schemeRepository.save(scheme);                                  // 1) insert → DB assigns id
  scheme.setSchemeCode("SCH" + String.format("%05d", scheme.getId()));     // 2) SCH + 5-digit zero-padded id → SCH00001
  return mapper.toResponse(schemeRepository.save(scheme));                  // 3) save the code
  ```
- **Institution** — [`InstitutionServiceImpl.create`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/InstitutionServiceImpl.java):
  ```java
  institution.setInstitutionCode("INST" + String.format("%05d", institution.getId()));   // INST00001
  ```
- **Sponsor** — [`SponsorServiceImpl.create`](Backend/core-service/src/main/java/com/granttrack/funding/service/impl/SponsorServiceImpl.java):
  ```java
  sponsor.setSponsorCode("SP" + String.format("%06d", sponsor.getId()));   // SP + 6 digits → SP000001
  ```
- Each code column is **unique** (e.g. `@Column(name = "scheme_code", unique = true, length = 20)` on
  [`FundingScheme.java`](Backend/core-service/src/main/java/com/granttrack/funding/entity/FundingScheme.java)).
- `String.format("%05d", 1)` → `"00001"`; scheme/institution use **5** digits, sponsor uses **6**.
- (There's a `@PrePersist generateCode()` on `Sponsor` that's an intentional **no-op** placeholder —
  the code can't be built at `@PrePersist` time because the id isn't assigned yet, so the service does
  it after the first save. Good thing to mention if asked "why two saves?".)
- The mapper exposes it: `FundingSchemeResponse.schemeCode` (etc.), and the list table shows it as the
  "ID" column (`{ key: 'schemeCode', header: 'ID' }`).

**Interview line:** *"The numeric primary key is internal; the `SCHxxxxx` code is a stable, readable
business identifier derived from that id (prefix + zero-padded), generated server-side and stored
unique. It needs a save-then-set-then-save because the id is DB-generated."*

---

## 8. Exception handling (frontend + backend)

Same contract as the rest of the app.
- **Backend:** services throw `BusinessException` (409 — e.g. "Only an UPCOMING call can be opened",
  "closeDate cannot be before openDate", invalid status), `ResourceNotFoundException` (404 — unknown
  sponsor/scheme id), bean-validation → 400 (e.g. bad phone `@Pattern`), `@PreAuthorize` → 403. All
  mapped to the standard envelope by [`GlobalExceptionHandler`](Backend/common-lib/src/main/java/com/granttrack/common/handler/GlobalExceptionHandler.java). File storage throws `BusinessException`
  for unsupported type / oversize / missing file.
- **Frontend:** the global [`error.interceptor.ts`](Frontend/src/app/core/interceptors/error.interceptor.ts) toasts the backend message; each form's `subscribe`
  error branch just resets `saving()`. Client-side `Validators` block a bad submit before it leaves
  the browser (`if (this.form.invalid) { this.form.markAllAsTouched(); return; }`).

Example: opening an already-open call → backend `BusinessException` → **409** → toast "Only an
UPCOMING call can be opened (current: OPEN)".

---

## 9. End-to-end walkthrough (rehearse out loud)

1. **Create a Sponsor** → `POST /funding/sponsors` → saved → code `SP000001` set → shows in the list.
2. **Create a Funding Scheme** → the form's Sponsor `<select>` was auto-loaded via `listSponsors`;
   pick a sponsor; choose category (or "Others" → type it); optionally attach a document.
   → `POST /funding/schemes` (stores the scheme, code `SCH00001`) → then
   `POST /funding/schemes/{id}/document` stores the file under `<upload-dir>/schemes/{id}/…` and saves
   the relative `documentPath`.
3. **Create a Grant Call** under that scheme → the call form's Scheme `<select>` was auto-loaded via
   `listSchemes({status:'ACTIVE'})`; call is created **UPCOMING**.
4. **Open the call** → the "Open" action (visible only for UPCOMING) → `POST /funding/calls/{id}/open`
   → guarded transition → status `OPEN`.
5. **Every researcher** now sees it under **Open Calls** (`listCalls({status:'OPEN'})`) and can
   **Apply**, which starts an application against that call.

---

## 10. Likely interview questions (crisp answers)

- **"How are the forms built?"** Angular reactive forms (`FormBuilder.nonNullable.group` + `Validators`);
  one `form` object reused for create/edit, distinguished by an `editingId` signal; `save()` branches
  to create vs update.
- **"How does the Sponsor dropdown get its data?"** `ngOnInit` calls `listSponsors()` into a `sponsors`
  signal; the template renders `@for (s of sponsors())` as `<option [ngValue]="s.id">{{ s.name }}</option>`.
- **"How does the 'Others' input work?"** `@if (form.get('category')?.value === 'Others')` reveals a
  `categoryOther` input; `save()` sends `categoryOther` instead of the literal "Others"; `openEdit`
  reverses it for values not in the fixed list.
- **"Where and how is an uploaded file stored?"** After the scheme is created, a multipart
  `POST /schemes/{id}/document` → `DocumentStorageService.storeSchemeDocument` writes to
  `<upload-dir>/schemes/{id}/<uuid>.ext` and returns the relative path, which is saved as
  `documentPath`. The root is the configured `granttrack.storage.upload-dir`.
- **"How does an open call reach researchers?"** Opening is a one-field status change to OPEN;
  researchers query calls with `status=OPEN`, so it appears immediately — no per-user feed.
- **"How are SCH/INST/SP codes generated?"** On the server after the first save: prefix +
  `String.format("%0Nd", id)` (SCH/INST use 5 digits, SP uses 6), stored in a unique code column.
- **"Why save twice when creating?"** The code is derived from the DB-generated id, which only exists
  after the first insert; so we insert, set the code, and save again.
- **"How does Edit work / how is it different from Create?"** Same modal + form + `save()`, switched by
  an `editingId` signal. `openEdit` pre-fills the form and reverse-maps "Others" values; `save()` sends
  PUT when an id is present, POST otherwise. Backend `update()` loads the managed entity and mutates it
  (dirty-check UPDATE), **doesn't** regenerate the code (single save), and only re-fetches the sponsor
  if the FK changed. The document is optional on edit — untouched unless a new file is picked.
- **"Where does funding live — is it a microservice?"** No — it's part of the monolithic core-service;
  it's reached through the gateway like everything else.

---

### File cheat-sheet (print this)
- **FE:** `features/funding/{schemes,sponsors,institutions,calls}/*-list.component.ts` (+ scheme/call `.html`),
  `features/funding/funding.service.ts`, `features/applications/opportunities/opportunities.component.ts`
- **core-service:** `funding/controller/{Sponsor,Institution,FundingScheme,GrantCall}Controller.java`,
  `funding/service/impl/{Sponsor,Institution,FundingScheme,GrantCall}ServiceImpl.java`,
  `funding/entity/{Sponsor,Institution,FundingScheme,GrantCall}.java`, `funding/mapper/FundingMapper.java`,
  `application/service/DocumentStorageService.java`
- **config:** `core-service/src/main/resources/application.yml` + `application-local.yml` (`granttrack.storage.upload-dir`)
- **shared:** `common-lib/.../handler/GlobalExceptionHandler.java`, `core/interceptors/{auth,error}.interceptor.ts`
