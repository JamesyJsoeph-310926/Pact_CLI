package com.ust.sdet.api.selenide;

import com.ust.sdet.api.selenide.config.Config;
import com.ust.sdet.api.selenide.pages.SelenideProductPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SeleindeProductTest {
    @BeforeAll
    static void setup() {
        Config.apply();
    }

    @Test
    void verifyRunningShoesPage() {
        SelenideProductPage productPage = new SelenideProductPage()
                .openPage("running-shoes");

    }
}
