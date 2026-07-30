package com.granttrack.notification.service.impl;

import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.common.security.SecurityUtils;
import com.granttrack.notification.dto.request.NotificationRequest;
import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.entity.Notification;
import com.granttrack.notification.entity.NotificationCategory;
import com.granttrack.notification.entity.NotificationStatus;
import com.granttrack.notification.mapper.NotificationMapper;
import com.granttrack.notification.repository.NotificationRepository;
import com.granttrack.notification.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForCurrentUser(String status, Pageable pageable) {
        Long userId = currentUserId();
        Specification<Notification> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return notificationRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCountForCurrentUser() {
        return notificationRepository.countByUserIdAndStatus(currentUserId(), NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = findOwned(id);
        notification.setStatus(NotificationStatus.READ);
        return mapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public NotificationResponse dismiss(Long id) {
        Notification notification = findOwned(id);
        notification.setStatus(NotificationStatus.DISMISSED);
        return mapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        return notify(request.userId(), request.message(), parseCategory(request.category()));
    }

    @Override
    @Transactional
    public NotificationResponse notify(Long userId, String message, NotificationCategory category) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .category(category)
                .status(NotificationStatus.UNREAD)
                .build();
        return mapper.toResponse(notificationRepository.save(notification));
    }

    private Notification findOwned(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!notification.getUserId().equals(currentUserId())) {
            throw new AccessDeniedException("Not your notification");
        }
        return notification;
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException("No authenticated user"));
    }

    private NotificationStatus parseStatus(String raw) {
        try {
            return NotificationStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid status: " + raw);
        }
    }

    private NotificationCategory parseCategory(String raw) {
        try {
            return NotificationCategory.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid category: " + raw);
        }
    }
}
