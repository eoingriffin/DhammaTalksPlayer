package com.dhammaplayer

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base test class providing common setup for all unit tests.
 *
 * Features:
 * - Configures coroutine test dispatcher for main thread
 * - Initializes MockK annotations
 * - Provides automatic cleanup after each test
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        // Set up test dispatcher for coroutines
        Dispatchers.setMain(testDispatcher)

        // Initialize MockK annotations
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Call child class setup
        onSetUp()
    }

    @AfterEach
    fun tearDown() {
        // Call child class teardown
        onTearDown()

        // Clean up mocks
        clearAllMocks()
        unmockkAll()

        // Reset main dispatcher
        Dispatchers.resetMain()
    }

    /**
     * Override this method to add test-specific setup logic.
     */
    protected open fun onSetUp() {}

    /**
     * Override this method to add test-specific teardown logic.
     */
    protected open fun onTearDown() {}

    /**
     * Helper property to access the test dispatcher for advancing time in tests.
     */
    protected val testScheduler
        get() = testDispatcher.scheduler
}

