package com.ust.sdet.api.selenide.config;

import com.codeborne.selenide.Configuration;

public class Config {
    public static void apply() {
        Configuration.baseUrl = "http://localhost:5173";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1440x900";
        Configuration.headless = false;
    }
}