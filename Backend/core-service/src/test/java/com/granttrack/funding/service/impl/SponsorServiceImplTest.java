package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.funding.dto.request.SponsorRequest;
import com.granttrack.funding.dto.response.SponsorResponse;
import com.granttrack.funding.entity.Sponsor;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.SponsorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SponsorServiceImplTest {

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private FundingMapper mapper;

    @InjectMocks
    private SponsorServiceImpl sponsorService;

    @Test
    void create_Success() {
        // Arrange
        SponsorRequest request = new SponsorRequest("Sponsor Name", "Government", "contact@test.com", "123", "Address", "www.test.com");
        Sponsor savedSponsor = Sponsor.builder().name("Sponsor Name").build();
        savedSponsor.setId(1L);
        SponsorResponse response = SponsorResponse.builder().id(1L).name("Sponsor Name").build();

        when(sponsorRepository.save(any(Sponsor.class))).thenReturn(savedSponsor);
        when(mapper.toResponse(savedSponsor)).thenReturn(response);

        // Act
        SponsorResponse result = sponsorService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals("Sponsor Name", result.name());
        // verify save is called twice: once for initial save, once after setting the code
        verify(sponsorRepository, times(2)).save(any(Sponsor.class));
    }

    @Test
    void update_Success() {
        // Arrange
        Long sponsorId = 1L;
        SponsorRequest request = new SponsorRequest("Updated Name", "Gov", "new@test.com", "12345", "New Address", "www.new.com");
        Sponsor existingSponsor = Sponsor.builder().name("Old Name").build();
        existingSponsor.setId(sponsorId);
        SponsorResponse response = SponsorResponse.builder().id(sponsorId).name("Updated Name").build();

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(existingSponsor));
        when(sponsorRepository.save(any(Sponsor.class))).thenReturn(existingSponsor);
        when(mapper.toResponse(existingSponsor)).thenReturn(response);

        // Act
        SponsorResponse result = sponsorService.update(sponsorId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.name());
        verify(sponsorRepository, times(1)).save(existingSponsor);
    }

    @Test
    void update_NotFound_ThrowsException() {
        Long sponsorId = 1L;
        SponsorRequest request = new SponsorRequest("Name", "Type", "a@a.com", "123", "Add", "Web");
        
        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sponsorService.update(sponsorId, request));
    }

    @Test
    void getById_Success() {
        // Arrange
        Long sponsorId = 1L;
        Sponsor sponsor = Sponsor.builder().name("Name").build();
        sponsor.setId(sponsorId);
        SponsorResponse response = SponsorResponse.builder().id(sponsorId).name("Name").build();

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(sponsor));
        when(mapper.toResponse(sponsor)).thenReturn(response);

        // Act
        SponsorResponse result = sponsorService.getById(sponsorId);

        // Assert
        assertNotNull(result);
        assertEquals(sponsorId, result.id());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(sponsorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sponsorService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Sponsor sponsor = Sponsor.builder().name("Sponsor Name").build();
        sponsor.setId(1L);
        SponsorResponse response = SponsorResponse.builder().id(1L).name("Sponsor Name").build();
        Page<Sponsor> page = new PageImpl<>(List.of(sponsor));

        when(sponsorRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(sponsor)).thenReturn(response);

        // Act
        Page<SponsorResponse> result = sponsorService.list("Sponsor", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Sponsor Name", result.getContent().get(0).name());
    }

    @Test
    void delete_Success() {
        // Arrange
        Long sponsorId = 1L;
        Sponsor sponsor = Sponsor.builder().build();
        sponsor.setId(sponsorId);
        sponsor.setDeleted(false);

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(sponsor));

        // Act
        sponsorService.delete(sponsorId);

        // Assert
        assertTrue(sponsor.isDeleted());
        verify(sponsorRepository, times(1)).save(sponsor);
    }
}
