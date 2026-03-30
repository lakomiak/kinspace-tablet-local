package com.adhdfocus.app.domain.gamification

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.ints.shouldBe
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import io.kotest.matchers.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Unit Tests for EfficiencyCalculator
 *
 * Tests verify:
 * - Eff