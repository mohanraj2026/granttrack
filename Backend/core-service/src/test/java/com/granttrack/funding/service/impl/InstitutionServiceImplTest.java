package com.granttrack.funding.service.impl;

import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.funding.dto.request.InstitutionRequest;
import com.granttrack.funding.dto.response.InstitutionResponse;
import com.granttrack.funding.entity.Institution;
import com.granttrack.funding.mapper.FundingMapper;
import com.granttrack.funding.repository.InstitutionRepository;
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
class InstitutionServiceImplTest {

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private FundingMapper mapper;

    @InjectMocks
    private InstitutionServiceImpl institutionService;

    @Test
    void create_Success() {
        // Arrange
        InstitutionRequest request = new InstitutionRequest("Name", "Type", "US", "Uni", "Addr", "City", "State", "123", "999", "email@t.com");
        Institution savedInst = Institution.builder().name("Name").build();
        savedInst.setId(1L);
        InstitutionResponse response = InstitutionResponse.builder().id(1L).name("Name").build();

        when(institutionRepository.save(any(Institution.class))).thenReturn(savedInst);
        when(mapper.toResponse(savedInst)).thenReturn(response);

        // Act
        InstitutionResponse result = institutionService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals("Name", result.name());
        verify(institutionRepository, times(2)).save(any(Institution.class));
    }

    @Test
    void update_Success() {
        // Arrange
        Long instId = 1L;
        InstitutionRequest request = new InstitutionRequest("Upd Name", "Type", "US", "Uni", "Addr", "City", "State", "123", "999", "email@t.com");
        Institution existingInst = Institution.builder().name("Old Name").build();
        existingInst.setId(instId);
        InstitutionResponse response = InstitutionResponse.builder().id(instId).name("Upd Name").build();

        when(institutionRepository.findById(instId)).thenReturn(Optional.of(existingInst));
        when(institutionRepository.save(any(Institution.class))).thenReturn(existingInst);
        when(mapper.toResponse(existingInst)).thenReturn(response);

        // Act
        InstitutionResponse result = institutionService.update(instId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Upd Name", result.name());
        verify(institutionRepository, times(1)).save(existingInst);
    }

    @Test
    void update_NotFound_ThrowsException() {
        Long instId = 1L;
        InstitutionRequest request = new InstitutionRequest("Name", "Type", "US", "Uni", "Addr", "City", "State", "123", "999", "email@t.com");
        
        when(institutionRepository.findById(instId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> institutionService.update(instId, request));
    }

    @Test
    void getById_Success() {
        // Arrange
        Long instId = 1L;
        Institution inst = Institution.builder().name("Name").build();
        inst.setId(instId);
        InstitutionResponse response = InstitutionResponse.builder().id(instId).name("Name").build();

        when(institutionRepository.findById(instId)).thenReturn(Optional.of(inst));
        when(mapper.toResponse(inst)).thenReturn(response);

        // Act
        InstitutionResponse result = institutionService.getById(instId);

        // Assert
        assertNotNull(result);
        assertEquals(instId, result.id());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(institutionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> institutionService.getById(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Institution inst = Institution.builder().name("Name").build();
        inst.setId(1L);
        InstitutionResponse response = InstitutionResponse.builder().id(1L).name("Name").build();
        Page<Institution> page = new PageImpl<>(List.of(inst));

        when(institutionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(inst)).thenReturn(response);

        // Act
        Page<InstitutionResponse> result = institutionService.list("Name", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Name", result.getContent().get(0).name());
    }

    @Test
    void delete_Success() {
        // Arrange
        Long instId = 1L;
        Institution inst = Institution.builder().build();
        inst.setId(instId);
        inst.setDeleted(false);

        when(institutionRepository.findById(instId)).thenReturn(Optional.of(inst));

        // Act
        institutionService.delete(instId);

        // Assert
        assertTrue(inst.isDeleted());
        verify(institutionRepository, times(1)).save(inst);
    }
}
