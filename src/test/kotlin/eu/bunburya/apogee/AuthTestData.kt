package eu.bunburya.apogee

import java.io.File

val CERTS = mutableListOf<Pair<File, File>>().apply {
    for (i in 0..5) {
        add(
            Pair(
                File("/home/alan/bin/apogee/src/test/resources/client_certs/$i/cert.pem"),
                File("/home/alan/bin/apogee/src/test/resources/client_certs/$i/key.pem")
            )
        )
    }
}

val FINGERPRINTS = listOf(
    "8ed39f497beb86e08e3079455d0359d74527c26d79d5df2c45a13f8af611509f",
    "3ecae5f560242d134733b6ab6197a1b2f81fd646c396b9f5097c56d96c7098ef",
    "8cdfca082f88c210748cf02551ed32d4de14bdd5ddf4bc9d0869f1ba01c43332",
    "8e0e8dc95e5a87edc26596d62b94691cfd985ccbaa2e87b3819936d7102be414",
    "6ab6843e83129cb02ddc54f4660d3ef8add2649bde8e298ef281b155111ab0c0"
)
