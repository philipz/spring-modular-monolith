package com.sivalabs.bookstore.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orders.backfill")
public class BackfillProperties {

    private boolean enabled = false;
    private Integer lookbackDays = 30;
    private Integer recordLimit = 1000;
    private Source source = new Source();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(Integer lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public Integer getRecordLimit() {
        return recordLimit;
    }

    public void setRecordLimit(Integer recordLimit) {
        this.recordLimit = recordLimit;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public static class Source {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
