# Blog

Spring Boot와 Thymeleaf로 만든 개인 블로그입니다.

## Tech Stack

| Category | Stack |
|----------|-------|
| Backend | Spring Boot 3.5.7, Spring MVC |
| View | Thymeleaf, Tailwind CSS |
| Markdown | flexmark-java 0.64.8 |
| Syntax Highlight | highlight.js (self-hosted) |
| Build | Gradle 9.2.0 |
| Runtime | JDK 21 |

## Features

- 마크다운(`.md`) 파일 기반 블로그 포스트
- 카테고리 / 태그 분류
- 다크모드 / 라이트모드 (테마 깜빡임 없음)
- 코드 하이라이팅
- 반응형 UI

## Getting Started

### Prerequisites

- JDK 21
- Gradle 9.2.0 (or use included `./gradlew`)

### Run

```bash
./gradlew bootRun
```

```
http://localhost:8080
```

### Build

```bash
./gradlew build
java -jar build/libs/spring-boot-thymeleaf-1.0.0.jar
```
