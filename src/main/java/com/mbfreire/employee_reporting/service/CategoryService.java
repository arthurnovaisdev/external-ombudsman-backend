package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.CategoryRequestDTO;
import com.mbfreire.employee_reporting.dto.response.CategoryResponseDTO;
import com.mbfreire.employee_reporting.entity.Category;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.ResourceNotFoundException;
import com.mbfreire.employee_reporting.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        String normalizedName = dto.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessRuleException(
                    "Já existe uma categoria com esse nome."
            );
        }

        Category category = Category.builder()
                .name(normalizedName)
                .active(dto.active())
                .build();

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> findAllActive(Pageable pageable) {
        return categoryRepository.findByActiveTrue(pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> findAllForAdmin(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(this::toDTO);
    }

    @Transactional
    public void deactivate(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada."
                ));

        if (!category.isActive()) {
            return;
        }

        category.setActive(false);
        categoryRepository.save(category);
    }

    private CategoryResponseDTO toDTO(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.isActive()
        );
    }
}