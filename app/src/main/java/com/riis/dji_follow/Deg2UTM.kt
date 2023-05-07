package com.riis.dji_follow

public class Deg2UTM(Lat: Double, Lon: Double) {
    var Easting: Double
    var Northing: Double
    var Zone: Int
    var Letter = 0.toChar()

    init {
        Zone = Math.floor(Lon / 6 + 31).toInt()
        Letter =
            if (Lat < -72) 'C' else if (Lat < -64) 'D' else if (Lat < -56) 'E' else if (Lat < -48) 'F' else if (Lat < -40) 'G' else if (Lat < -32) 'H' else if (Lat < -24) 'J' else if (Lat < -16) 'K' else if (Lat < -8) 'L' else if (Lat < 0) 'M' else if (Lat < 8) 'N' else if (Lat < 16) 'P' else if (Lat < 24) 'Q' else if (Lat < 32) 'R' else if (Lat < 40) 'S' else if (Lat < 48) 'T' else if (Lat < 56) 'U' else if (Lat < 64) 'V' else if (Lat < 72) 'W' else 'X'
        Easting = 0.5 * Math.log(
            (1 + Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(
                Lat * Math.PI / 180
            ) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))
        ) * 0.9996 * 6399593.62 / Math.pow(
            1 + Math.pow(0.0820944379, 2.0) * Math.pow(Math.cos(Lat * Math.PI / 180), 2.0), 0.5
        ) * (1 + Math.pow(0.0820944379, 2.0) / 2 * Math.pow(
            0.5 * Math.log(
                (1 + Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(
                    Lat * Math.PI / 180
                ) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))
            ), 2.0
        ) * Math.pow(
            Math.cos(Lat * Math.PI / 180), 2.0
        ) / 3) + 500000
        Easting = Math.round(Easting * 100) * 0.01
        Northing = (Math.atan(
            Math.tan(Lat * Math.PI / 180) / Math.cos(
                Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180
            )
        ) - Lat * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(
            1 + 0.006739496742 * Math.pow(
                Math.cos(Lat * Math.PI / 180), 2.0
            )
        ) * (1 + 0.006739496742 / 2 * Math.pow(
            0.5 * Math.log(
                (1 + Math.cos(Lat * Math.PI / 180) * Math.sin(
                    Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180
                )) / (1 - Math.cos(Lat * Math.PI / 180) * Math.sin(Lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))
            ), 2.0
        ) * Math.pow(
            Math.cos(Lat * Math.PI / 180), 2.0
        )) + 0.9996 * 6399593.625 * (Lat * Math.PI / 180 - 0.005054622556 * (Lat * Math.PI / 180 + Math.sin(
            2 * Lat * Math.PI / 180
        ) / 2) + 4.258201531e-05 * (3 * (Lat * Math.PI / 180 + Math.sin(2 * Lat * Math.PI / 180) / 2) + Math.sin(
            2 * Lat * Math.PI / 180
        ) * Math.pow(
            Math.cos(Lat * Math.PI / 180), 2.0
        )) / 4 - 1.674057895e-07 * (5 * (3 * (Lat * Math.PI / 180 + Math.sin(2 * Lat * Math.PI / 180) / 2) + Math.sin(
            2 * Lat * Math.PI / 180
        ) * Math.pow(
            Math.cos(Lat * Math.PI / 180), 2.0
        )) / 4 + Math.sin(2 * Lat * Math.PI / 180) * Math.pow(
            Math.cos(Lat * Math.PI / 180), 2.0
        ) * Math.pow(Math.cos(Lat * Math.PI / 180), 2.0)) / 3)
        if (Letter < 'M') Northing = Northing + 10000000
        Northing = Math.round(Northing * 100) * 0.01
    }
}