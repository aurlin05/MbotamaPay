package com.mbotamapay.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AppPropertiesTest {

    @Autowired
    private AppProperties appProperties;

    @Test
    void shouldLoadJwtProperties() {
        assertThat(appProperties.getJwt()).isNotNull();
        assertThat(appProperties.getJwt().getSecret()).isNotBlank();
        assertThat(appProperties.getJwt().getExpirationMs()).isPositive();
    }

    @Test
    void shouldLoadCinetPayProperties() {
        assertThat(appProperties.getCinetpay()).isNotNull();
        assertThat(appProperties.getCinetpay().getApiKey()).isNotBlank();
        assertThat(appProperties.getCinetpay().getSiteId()).isNotBlank();
        assertThat(appProperties.getCinetpay().getBaseUrl()).isNotBlank();
        assertThat(appProperties.getCinetpay().getNotifyUrl()).isNotBlank();
        assertThat(appProperties.getCinetpay().getReturnUrl()).isNotBlank();
    }

    @Test
    void shouldLoadMailProperties() {
        assertThat(appProperties.getMail()).isNotNull();
        assertThat(appProperties.getMail().getFrom()).isNotBlank();
    }

    @Test
    void shouldValidateJwtSecretLength() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("short");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);
        props.setCinetpay(new AppProperties.CinetPayPropertiesNested());
        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret must be at least 32 characters");
    }

    @Test
    void shouldValidateJwtExpirationIsPositive() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("this_is_a_valid_secret_key_with_more_than_32_characters");
        jwt.setExpirationMs(-1L);
        props.setJwt(jwt);
        props.setCinetpay(new AppProperties.CinetPayPropertiesNested());
        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT expiration time must be positive");
    }

    @Test
    void shouldFailWhenJwtSecretIsMissing() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);
        props.setCinetpay(new AppProperties.CinetPayPropertiesNested());
        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is required");
    }

    @Test
    void shouldFailWhenCinetPayApiKeyIsMissing() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("this_is_a_valid_secret_key_with_more_than_32_characters");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);

        AppProperties.CinetPayPropertiesNested cinetpay = new AppProperties.CinetPayPropertiesNested();
        cinetpay.setApiKey("");
        cinetpay.setSiteId("SITE_ID");
        cinetpay.setBaseUrl("https://api.cinetpay.com");
        cinetpay.setNotifyUrl("http://localhost/notify");
        cinetpay.setReturnUrl("http://localhost/return");
        props.setCinetpay(cinetpay);

        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CinetPay API key is required");
    }

    @Test
    void shouldFailWhenCinetPaySiteIdIsMissing() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("this_is_a_valid_secret_key_with_more_than_32_characters");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);

        AppProperties.CinetPayPropertiesNested cinetpay = new AppProperties.CinetPayPropertiesNested();
        cinetpay.setApiKey("API_KEY");
        cinetpay.setSiteId("");
        cinetpay.setBaseUrl("https://api.cinetpay.com");
        cinetpay.setNotifyUrl("http://localhost/notify");
        cinetpay.setReturnUrl("http://localhost/return");
        props.setCinetpay(cinetpay);

        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CinetPay site ID is required");
    }

    @Test
    void shouldFailWhenMailFromIsMissing() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("this_is_a_valid_secret_key_with_more_than_32_characters");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);

        AppProperties.CinetPayPropertiesNested cinetpay = new AppProperties.CinetPayPropertiesNested();
        cinetpay.setApiKey("API_KEY");
        cinetpay.setSiteId("SITE_ID");
        cinetpay.setBaseUrl("https://api.cinetpay.com");
        cinetpay.setNotifyUrl("http://localhost/notify");
        cinetpay.setReturnUrl("http://localhost/return");
        props.setCinetpay(cinetpay);

        AppProperties.MailProperties mail = new AppProperties.MailProperties();
        mail.setFrom("");
        props.setMail(mail);

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mail from address is required");
    }

    @Test
    void shouldFailWhenJwtConfigurationIsNull() {
        AppProperties props = new AppProperties();
        props.setJwt(null);
        props.setCinetpay(new AppProperties.CinetPayPropertiesNested());
        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT configuration is missing");
    }

    @Test
    void shouldFailWhenCinetPayConfigurationIsNull() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("this_is_a_valid_secret_key_with_more_than_32_characters");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);
        props.setCinetpay(null);
        props.setMail(new AppProperties.MailProperties());

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CinetPay configuration is missing");
    }

    @Test
    void shouldFailWhenMailConfigurationIsNull() {
        AppProperties props = new AppProperties();
        AppProperties.JwtProperties jwt = new AppProperties.JwtProperties();
        jwt.setSecret("this_is_a_valid_secret_key_with_more_than_32_characters");
        jwt.setExpirationMs(3600000L);
        props.setJwt(jwt);

        AppProperties.CinetPayPropertiesNested cinetpay = new AppProperties.CinetPayPropertiesNested();
        cinetpay.setApiKey("API_KEY");
        cinetpay.setSiteId("SITE_ID");
        cinetpay.setBaseUrl("https://api.cinetpay.com");
        cinetpay.setNotifyUrl("http://localhost/notify");
        cinetpay.setReturnUrl("http://localhost/return");
        props.setCinetpay(cinetpay);

        props.setMail(null);

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mail configuration is missing");
    }
}
