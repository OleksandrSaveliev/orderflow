package com.my.orderflow.mapper;

import com.my.orderflow.dto.product.ProductRequestDto;
import com.my.orderflow.dto.product.ProductResponseDto;
import com.my.orderflow.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequestDto dto);

    ProductResponseDto toResponse(Product entity);
}