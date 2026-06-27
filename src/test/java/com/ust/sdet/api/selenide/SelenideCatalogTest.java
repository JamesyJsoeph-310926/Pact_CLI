package com.ust.sdet.api.selenide;

import com.ust.sdet.api.selenide.config.Config;
import com.ust.sdet.api.selenide.pages.SelenideCatalogPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.page;

public class SelenideCatalogTest {

    @BeforeAll
    static void setup() {
        Config.apply();
    }

    @Test
    @DisplayName("Verify search returns exactly 1 products")
    void verifyResultCount() {
        SelenideCatalogPage page = page(SelenideCatalogPage.class);
        page.openPage().searchFor("headphones");
        page.results().shouldHave(size(1));
    }

    @Test
    @DisplayName("Verify product names are displayed in correct order")
    void verifyProductNamesOrder() {
        SelenideCatalogPage page = page(SelenideCatalogPage.class);
        page.openPage().searchFor("Pro");
        page.productTitles().shouldHave(texts( "Insulated Water Bottle", "Rain Jacket", "Kids Learning Tablet"));
    }

    @Test
    @DisplayName("Verify only one product contains Pro")
    void verifyOnlyOneProProduct() {
        SelenideCatalogPage page = page(SelenideCatalogPage.class);
        page.openPage().searchFor("Pro");
        page.results().filterBy(text("Rain Jacket")).shouldHave(size(1));
    }
}