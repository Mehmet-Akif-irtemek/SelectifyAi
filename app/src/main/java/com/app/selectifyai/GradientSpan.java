package com.app.selectifyai;

import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;

public class GradientSpan extends CharacterStyle implements UpdateAppearance {

    private final int[] colors;
    private final String targetText;

    public GradientSpan(int[] colors, String targetText) {
        this.colors = colors;
        this.targetText = targetText;
    }

    @Override
    public void updateDrawState(TextPaint paint) {
        float width = paint.measureText(targetText);
        Shader shader = new LinearGradient(
                0, 0, width, paint.getTextSize(),
                colors, null, Shader.TileMode.CLAMP);
        paint.setShader(shader);
    }
}