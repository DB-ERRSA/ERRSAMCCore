# Compatibility Paper

Sends Paper plugin/compatibility reports to the ERRSA bot at the configured HTTPS endpoint. The default report interval is 300 seconds. The receiver uses the matching shared secret.

Source: `com.errsa.compat.PaperReporter`.

## Settings

Default values from `paper/src/main/resources/modules/compatibility-paper/config.yml`:

```yaml
endpoint: "https://YOUR-BOT-HOST/compat/report"
secret: "REPLACE_WITH_LONG_RANDOM_SECRET"
report-interval-seconds: 300
```
