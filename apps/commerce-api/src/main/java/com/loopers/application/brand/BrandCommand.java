package com.loopers.application.brand;

public class BrandCommand {

    public record Create(
        String name,
        String description,
        String logoImageUrl
    ) {}

    public record Update(
        String name,
        String description,
        String logoImageUrl
    ) {}
}