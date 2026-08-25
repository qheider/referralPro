package com.actpro.referral.referral;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferralQrCodeServiceTest {

    private final ReferralQrCodeService referralQrCodeService = new ReferralQrCodeService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(referralQrCodeService, "baseUrl", "https://app.example.com");
    }

    @Test
    void shouldEncodeTheSameRedirectUrlThatClickingTheLinkWouldHit() throws Exception {
        byte[] png = referralQrCodeService.generatePng("AbcDef1234567890");

        assertEquals("https://app.example.com/r/AbcDef1234567890", decode(png));
    }

    @Test
    void shouldRenderAtTheRequestedSize() throws IOException {
        byte[] png = referralQrCodeService.generatePng("AbcDef1234567890", 512);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
    }

    @Test
    void shouldProduceValidPngBytes() {
        byte[] png = referralQrCodeService.generatePng("AbcDef1234567890");

        assertTrue(png.length > 0);
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
    }

    private String decode(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }
}
