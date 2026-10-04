package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RaceRepository
import com.example.model.CarCatalog
import com.example.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Apex Racer", appName)
  }

  @Test
  fun `car catalog contains default car`() {
    val cars = CarCatalog.ALL_CARS
    assertTrue(cars.isNotEmpty())
    val defaultCar = CarCatalog.getCar("car_interceptor")
    assertNotNull(defaultCar)
    assertEquals("Interceptor GT", defaultCar.name)
    assertTrue(defaultCar.isUnlockedByDefault)
  }

  @Test
  fun `repository persists cash and cars`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RaceRepository(context)

    val initialCash = repo.getCash()
    assertTrue(initialCash >= 0)

    val newCash = repo.addCash(500)
    assertEquals(initialCash + 500, newCash)

    repo.unlockCar("car_viper")
    assertTrue(repo.getUnlockedCars().contains("car_viper"))

    val isNewHigh = repo.updateHighScore(GameMode.ENDLESS_TRAFFIC, 1200)
    assertTrue(isNewHigh)
    assertEquals(1200, repo.getHighScore(GameMode.ENDLESS_TRAFFIC))
  }
}
