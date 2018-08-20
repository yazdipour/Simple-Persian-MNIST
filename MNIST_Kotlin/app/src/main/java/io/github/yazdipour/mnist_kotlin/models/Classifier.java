package io.github.yazdipour.mnist_kotlin.models;

public interface Classifier {
    String name();

    Classification recognize(final float[] pixels);
}
