# Copilot Instructions

Spring Boot + Thymeleaf 기반 개인 블로그.

## Stack

- Backend: Spring Boot, Java, Gradle
- Template: Thymeleaf
- Frontend: Tailwind CSS, Vanilla JS
- DB: MyBatis

## 주의사항

- `th:utext` 사용 시 반드시 `HtmlSanitizerService`를 거쳐야 함 (XSS)
- front matter 파싱 결과는 null 가능 필드가 많음
