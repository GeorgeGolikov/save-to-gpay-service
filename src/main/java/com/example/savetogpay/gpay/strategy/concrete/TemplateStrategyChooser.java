package com.example.savetogpay.gpay.strategy.concrete;

import com.example.savetogpay.gpay.strategy.TemplateStrategy;

import java.lang.reflect.Constructor;

public class TemplateStrategyChooser {
    private static final String TEMPLATE_STRATEGY_CONCRETE_CLASS_PATH_TEMPLATE =
            "com.example.savetogpay.gpay.strategy.concrete.%sTemplateStrategy";

    public static TemplateStrategy choose(String type) throws Exception {
        try {
            String strategyTypeFirstWord = getStrategyTypeInCamelCase(type);
            String className = getStrategyTypeConcreteClassName(strategyTypeFirstWord);
            Constructor<?> concreteStrategyClassConstructor = getConcreteStrategyClassConstructor(className);
            return (TemplateStrategy) concreteStrategyClassConstructor.newInstance();
        } catch (Exception e) {
            throw new Exception("Card type specified incorrectly.");
        }
    }

    private static String getStrategyTypeInCamelCase(String type) {
        String typeLowerCase = type.toLowerCase();
        String firstLetter = typeLowerCase.substring(0, 1);
        return typeLowerCase.replaceFirst(firstLetter, firstLetter.toUpperCase());
    }

    private static String getStrategyTypeConcreteClassName(String strategyTypeFirstWord) {
        return String.format(TEMPLATE_STRATEGY_CONCRETE_CLASS_PATH_TEMPLATE, strategyTypeFirstWord);
    }

    private static Constructor<?> getConcreteStrategyClassConstructor(String className) throws Exception {
        Class<?> concreteStrategyClass = Class.forName(className);
        return concreteStrategyClass.getDeclaredConstructor();
    }
}