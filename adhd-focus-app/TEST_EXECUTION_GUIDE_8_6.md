# Test Execution Guide - Task 8.6: Affirmation Display UI Component

## Overview

This guide provides instructions for running the integration and unit tests created for the AffirmationDisplay component.

## Test Files

### Integration Tests
- **File**: `src/androidTest/kotlin/com/adhdfocus/app/ui/common/component/AffirmationDisplayIntegrationTest.kt`
- **Type**: Android Instrumented Tests (requires Android device or emulator)
- **Count**: 60+ tests
- **Framework**: Jetpack Compose Test Framework + JUnit4

### Unit Tests
- **File**: `src/test/kotlin/com/adhdfocus/app/ui/common/component/AffirmationDisplayUnitTest.kt`
- **Type**: JVM Unit Tests (runs on local machine)
- **Count**: 40+ tests
- **Framework**: Kotest + Property-Based Testing

## Running Tests

### Prerequisites

1. **For Integration Tests**:
   - Android device or emulator connected
   - Android SDK installed
   - Gradle build system configured

2. **For Unit Tests**:
   - JVM installed
   - Gradle build system configured

### Running Unit Tests

#### Using Gradle (Recommended)

```bash
# Run all unit tests in the component package
./gradlew test --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayUnitTest"

# Run specific test
./gradlew test --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayUnitTest.testTaskCompleteAffirmationHasRequiredFields"

# Run with verbose output
./gradlew test --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayUnitTest" --info
```

#### Using IDE

1. Open the test file in Android Studio
2. Right-click on the test class or method
3. Select "Run" or "Run with Coverage"

### Running Integration Tests

#### Using Gradle (Recommended)

```bash
# Run all integration tests
./gradlew connectedAndroidTest --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayIntegrationTest"

# Run specific test
./gradlew connectedAndroidTest --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayIntegrationTest.testTaskCompleteAffirmationDisplays"

# Run with verbose output
./gradlew connectedAndroidTest --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayIntegrationTest" --info
```

#### Using IDE

1. Open the test file in Android Studio
2. Ensure Android device/emulator is connected
3. Right-click on the test class or method
4. Select "Run" or "Run with Coverage"

### Running All Tests

```bash
# Run all unit tests
./gradlew test

# Run all integration tests
./gradlew connectedAndroidTest

# Run all tests (unit + integration)
./gradlew test connectedAndroidTest
```

## Test Categories

### Unit Tests (40+ tests)

#### 1. Task Complete Affirmation Tests (6 tests)
- `testTaskCompleteAffirmationHasRequiredFields`
- `testTaskCompleteAffirmationWithDefaultStreakCount`
- `testTaskCompleteAffirmationWithCustomStreakCount`
- `testTaskCompleteAffirmationMessageIsNonEmpty`
- `testTaskCompleteAffirmationTaskIdIsNonEmpty`
- `testTaskCompleteAffirmationTimestampIsValid`

#### 2. Day Complete Affirmation Tests (3 tests)
- `testDayCompleteAffirmationHasRequiredFields`
- `testDayCompleteAffirmationMessageIsNonEmpty`
- `testDayCompleteAffirmationTimestampIsValid`

#### 3. Streak Milestone Affirmation Tests (4 tests)
- `testStreakMilestoneAffirmationHasRequiredFields`
- `testStreakMilestoneAffirmationMessageIsNonEmpty`
- `testStreakMilestoneAffirmationWithVariousStreakCounts`
- `testStreakMilestoneAffirmationTimestampIsValid`

#### 4. Affirmation Type Tests (3 tests)
- `testTaskCompleteIsInstanceOfAffirmationEvent`
- `testDayCompleteIsInstanceOfAffirmationEvent`
- `testStreakMilestoneIsInstanceOfAffirmationEvent`

#### 5. Message Content Tests (5 tests)
- `testAffirmationMessagesCanContainSpecialCharacters`
- `testAffirmationMessagesCanContainUnicodeCharacters`
- `testAffirmationMessagesCanContainNewlines`
- `testAffirmationMessagesCanBeVeryLong`
- `testAffirmationMessagesCanBeVeryShort`

#### 6. Timestamp Tests (3 tests)
- `testAffirmationTimestampIsNonZero`
- `testAffirmationTimestampIsPositive`
- `testAffirmationTimestampIsRecent`

#### 7. Task ID Tests (1 test)
- `testTaskCompleteAffirmationTaskIdIsPreserved`

#### 8. Streak Count Tests (2 tests)
- `testStreakMilestoneAffirmationStreakCountIsPreserved`
- `testTaskCompleteAffirmationStreakCountIsPreserved`

#### 9. Equality Tests (5 tests)
- `testTaskCompleteAffirmationsWithSameDataAreEqual`
- `testDayCompleteAffirmationsWithSameDataAreEqual`
- `testStreakMilestoneAffirmationsWithSameDataAreEqual`
- `testTaskCompleteAffirmationsWithDifferentMessagesAreNotEqual`
- `testTaskCompleteAffirmationsWithDifferentTaskIdsAreNotEqual`
- `testStreakMilestoneAffirmationsWithDifferentStreakCountsAreNotEqual`

#### 10. Data Class Tests (3 tests)
- `testTaskCompleteAffirmationCanBeCopiedWithNewMessage`
- `testDayCompleteAffirmationCanBeCopiedWithNewMessage`
- `testStreakMilestoneAffirmationCanBeCopiedWithNewStreakCount`

#### 11. String Representation Tests (3 tests)
- `testTaskCompleteAffirmationHasStringRepresentation`
- `testDayCompleteAffirmationHasStringRepresentation`
- `testStreakMilestoneAffirmationHasStringRepresentation`

### Integration Tests (60+ tests)

#### 1. Task Completion Affirmation Tests (5 tests)
- `testTaskCompleteAffirmationDisplays`
- `testTaskCompleteAffirmationWithVariousMessages`
- `testTaskCompleteAffirmationWithDifferentTaskIds`
- `testTaskCompleteAffirmationWithStreakCount`

#### 2. Day Completion Affirmation Tests (2 tests)
- `testDayCompleteAffirmationDisplays`
- `testDayCompleteAffirmationWithVariousMessages`

#### 3. Streak Milestone Affirmation Tests (3 tests)
- `testStreakMilestoneAffirmationDisplays`
- `testStreakMilestoneAffirmationWithVariousStreakCounts`
- `testStreakMilestoneAffirmationWithVariousMessages`

#### 4. Auto-Dismiss Tests (2 tests)
- `testAffirmationAutoDismissesAfterDelay`
- `testMultipleAffirmationsDisplaySequentially`

#### 5. Null Affirmation Tests (2 tests)
- `testNullAffirmationDoesNotDisplay`
- `testAffirmationDisplaysAfterNullState`

#### 6. Styling and Accessibility Tests (3 tests)
- `testTaskCompleteAffirmationHasCorrectStyling`
- `testDayCompleteAffirmationHasCorrectStyling`
- `testStreakMilestoneAffirmationHasCorrectStyling`

#### 7. Animation Tests (2 tests)
- `testAffirmationDisplaysWithAnimation`
- `testAffirmationAnimationOnMultipleDisplays`

#### 8. Message Content Tests (2 tests)
- `testAffirmationWithLongMessage`
- `testAffirmationWithShortMessage`

#### 9. Timestamp Tests (2 tests)
- `testAffirmationWithCurrentTimestamp`
- `testAffirmationWithPastTimestamp`

#### 10. Modifier Tests (1 test)
- `testAffirmationDisplayWithCustomModifier`

#### 11. Callback Tests (2 tests)
- `testOnDismissCallbackInvoked`
- `testOnDismissCallbackWithMultipleAffirmations`

#### 12. Edge Case Tests (3 tests)
- `testAffirmationWithEmptyStringMessage`
- `testAffirmationWithSpecialCharacters`
- `testAffirmationWithUnicodeCharacters`
- `testAffirmationWithNewlines`

#### 13. Rapid Change Tests (2 tests)
- `testRapidAffirmationChanges`
- `testAffirmationTypeChanges`

## Expected Results

### Unit Tests
- All 40+ tests should pass
- No compilation errors
- No runtime errors
- Property-based tests should generate multiple test cases

### Integration Tests
- All 60+ tests should pass
- Component should display correctly on device/emulator
- Animations should be smooth
- Auto-dismiss timing should be accurate
- Manual dismissal should work

## Troubleshooting

### Common Issues

#### 1. Tests Won't Compile
- **Cause**: Missing dependencies or incorrect imports
- **Solution**: Run `./gradlew clean build` to rebuild the project

#### 2. Integration Tests Won't Run
- **Cause**: No Android device/emulator connected
- **Solution**: Connect a device or start an emulator before running tests

#### 3. Tests Timeout
- **Cause**: Device/emulator is slow or unresponsive
- **Solution**: Increase timeout in gradle.properties or use a faster device

#### 4. Animation Tests Fail
- **Cause**: Device animation settings are disabled
- **Solution**: Enable animations in device developer settings

### Debug Mode

To run tests with debug output:

```bash
# Unit tests with debug output
./gradlew test --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayUnitTest" --debug

# Integration tests with debug output
./gradlew connectedAndroidTest --tests "com.adhdfocus.app.ui.common.component.AffirmationDisplayIntegrationTest" --debug
```

## Test Coverage

### Code Coverage Report

To generate a code coverage report:

```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html
```

### Coverage Goals

- **Unit Tests**: 100% coverage of AffirmationEvent data classes
- **Integration Tests**: 100% coverage of AffirmationDisplay component
- **Overall**: 95%+ coverage of affirmation-related code

## Continuous Integration

### GitHub Actions

Tests are automatically run on:
- Push to main branch
- Pull requests
- Scheduled daily builds

### Local CI

To simulate CI locally:

```bash
# Run all tests as CI would
./gradlew clean test connectedAndroidTest --info
```

## Performance Benchmarks

### Expected Performance

- **Unit Tests**: < 30 seconds total
- **Integration Tests**: < 2 minutes total
- **All Tests**: < 3 minutes total

### Performance Optimization

If tests are slow:

1. Use a faster device/emulator
2. Run tests in parallel: `./gradlew test -x connectedAndroidTest --parallel`
3. Use test sharding for large test suites

## Additional Resources

- [Jetpack Compose Testing Documentation](https://developer.android.com/jetpack/compose/testing)
- [Kotest Documentation](https://kotest.io/)
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Gradle Testing Documentation](https://docs.gradle.org/current/userguide/testing_java_project.html)

## Contact & Support

For issues or questions about these tests:
1. Check the test file comments for detailed explanations
2. Review the implementation summary in TASK_8_6_IMPLEMENTATION.md
3. Consult the project's testing guidelines
