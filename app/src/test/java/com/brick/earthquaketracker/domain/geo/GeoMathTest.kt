package com.brick.earthquaketracker.domain.geo

import com.brick.earthquaketracker.domain.model.Bearing
import com.brick.earthquaketracker.domain.model.Coordinates
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoMathTest {

    // --- Haversine distance ---

    @Test
    fun `London to Paris is approximately 344 km`() {
        val london = Coordinates(51.5074, -0.1278)
        val paris = Coordinates(48.8566, 2.3522)
        val distance = GeoMath.haversineKm(london, paris)
        assertThat(distance).isWithin(5.0).of(344.0)
    }

    @Test
    fun `New York to Los Angeles is approximately 3944 km`() {
        val nyc = Coordinates(40.7128, -74.0060)
        val la = Coordinates(34.0522, -118.2437)
        val distance = GeoMath.haversineKm(nyc, la)
        assertThat(distance).isWithin(50.0).of(3944.0)
    }

    @Test
    fun `identical points have zero distance`() {
        val point = Coordinates(35.6762, 139.6503) // Tokyo
        val distance = GeoMath.haversineKm(point, point)
        assertThat(distance).isWithin(0.001).of(0.0)
    }

    @Test
    fun `equator antipodes are approximately half the circumference`() {
        val a = Coordinates(0.0, 0.0)
        val b = Coordinates(0.0, 180.0)
        val distance = GeoMath.haversineKm(a, b)
        // Half of Earth's circumference ≈ 20,015 km
        assertThat(distance).isWithin(50.0).of(20_015.0)
    }

    @Test
    fun `antimeridian crossing - Tokyo to Honolulu`() {
        val tokyo = Coordinates(35.6762, 139.6503)
        val honolulu = Coordinates(21.3069, -157.8583)
        val distance = GeoMath.haversineKm(tokyo, honolulu)
        // Known distance ≈ 6,196 km
        assertThat(distance).isWithin(50.0).of(6_196.0)
    }

    @Test
    fun `north pole to south pole is approximately half the circumference`() {
        val northPole = Coordinates(90.0, 0.0)
        val southPole = Coordinates(-90.0, 0.0)
        val distance = GeoMath.haversineKm(northPole, southPole)
        assertThat(distance).isWithin(50.0).of(20_015.0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = Coordinates(51.5074, -0.1278)
        val b = Coordinates(-33.8688, 151.2093)
        assertThat(GeoMath.haversineKm(a, b))
            .isWithin(0.01)
            .of(GeoMath.haversineKm(b, a))
    }

    // --- Bearing ---

    @Test
    fun `due north bearing is 0 degrees`() {
        val from = Coordinates(0.0, 0.0)
        val to = Coordinates(10.0, 0.0)
        val bearing = GeoMath.bearingDegrees(from, to)
        assertThat(bearing).isWithin(0.1).of(0.0)
    }

    @Test
    fun `due east bearing is 90 degrees`() {
        val from = Coordinates(0.0, 0.0)
        val to = Coordinates(0.0, 10.0)
        val bearing = GeoMath.bearingDegrees(from, to)
        assertThat(bearing).isWithin(0.1).of(90.0)
    }

    @Test
    fun `due south bearing is 180 degrees`() {
        val from = Coordinates(10.0, 0.0)
        val to = Coordinates(0.0, 0.0)
        val bearing = GeoMath.bearingDegrees(from, to)
        assertThat(bearing).isWithin(0.1).of(180.0)
    }

    @Test
    fun `due west bearing is 270 degrees`() {
        val from = Coordinates(0.0, 10.0)
        val to = Coordinates(0.0, 0.0)
        val bearing = GeoMath.bearingDegrees(from, to)
        assertThat(bearing).isWithin(0.1).of(270.0)
    }

    // --- Cardinal conversion ---

    @Test
    fun `0 degrees maps to N`() {
        assertThat(GeoMath.toCardinal(0.0)).isEqualTo(Bearing.N)
    }

    @Test
    fun `45 degrees maps to NE`() {
        assertThat(GeoMath.toCardinal(45.0)).isEqualTo(Bearing.NE)
    }

    @Test
    fun `90 degrees maps to E`() {
        assertThat(GeoMath.toCardinal(90.0)).isEqualTo(Bearing.E)
    }

    @Test
    fun `180 degrees maps to S`() {
        assertThat(GeoMath.toCardinal(180.0)).isEqualTo(Bearing.S)
    }

    @Test
    fun `270 degrees maps to W`() {
        assertThat(GeoMath.toCardinal(270.0)).isEqualTo(Bearing.W)
    }

    @Test
    fun `350 degrees maps to N (wraps around)`() {
        assertThat(GeoMath.toCardinal(350.0)).isEqualTo(Bearing.N)
    }

    @Test
    fun `22 degrees maps to NNE`() {
        assertThat(GeoMath.toCardinal(22.0)).isEqualTo(Bearing.NNE)
    }

    @Test
    fun `200 degrees maps to SSW`() {
        assertThat(GeoMath.toCardinal(200.0)).isEqualTo(Bearing.SSW)
    }
}
