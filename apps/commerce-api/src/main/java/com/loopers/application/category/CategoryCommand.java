package com.loopers.application.category;

public class CategoryCommand {

    public record Create(
        String name,
        Long parentId
    ) {}

    public record Update(
        String name
    ) {}
}