package com.ust.sdet.api.selenide.pages;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

import static com.codeborne.selenide.Selenide.$;

public class SelenideProductPage {
    private final SelenideElement product_title = $("[data-test='detail-name']");
    private final SelenideElement product_description = $(".lead");
    private final SelenideElement price = $(".price");


    public SelenideProductPage openPage(String pdt){
        open("/product/"+pdt);
        product_title.shouldBe(visible);
        return this;
    }

    public String getProductTitle() {
        return product_title.getText();
    }

    public String getProductDescription() {
        return product_description.getText();
    }

    public String getPrice() {
        return price.getText();
    }



}
