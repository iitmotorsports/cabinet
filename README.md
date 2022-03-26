<div align="center">

# cabinet

<p>
  <b>A web-based logging storage system for FSAE.</b>
</p>

[![](https://github.com/illinois-tech-motorsports/cabinet/actions/workflows/build.yml/badge.svg)](https://github.com/illinois-tech-motorsports/cabinet/actions/workflows/build.yml)
[![](https://img.shields.io/github/license/illinois-tech-motorsports/cabinet)](https://github.com/illinois-tech-motorsports/cabinet/blob/main/LICENSE)
[![](https://img.shields.io/tokei/lines/github/illinois-tech-motorsports/cabinet)](https://github.com/illinois-tech-motorsports/cabinet)
</div>

## About
Cabinet is a web-based storage system for log files and statistics. The concept behind cabinet was to provide an all-in-one file server, statistic parser, and GUI. Logs from the [dashboard](https://github.com/illinois-tech-motorsports/dashboard-2022) app are recorded and then uploaded to the cabinet server. Each post includes a plain-text file of the console output, a binary file which tracks statistics from the car, and a mapping file to map the statistics to a pretty name.

## Usage
Cabinet exposes an easy-to-use REST API for posting / reading logs.

### **POST** `/api/v1/logs`

Params:
* `date` = Epoch Time (UTC) of when the posted log was created.

Body:
* `log` (File) = Plain-text log file
* `stats` (File) = Binary log file
* `stats_map` (File) = Map for binary log file

Example response:
```json
{
    "id": 0,
    "date": 1648336488,
    "uploadDate": 1648319097
}
```
Errors:
* 400 Bad Request
  * No `date` parameter was supplied.
  * No `log` file was included in the body.
  * The `stats` file was included, but the `stats_map` file was missing.

### **GET** `/api/v1/logs`

Example response:
```json
[
    {
        "id": 1,
        "date": 1648336488,
        "uploadDate": 1648319545,
        "size": "8.3 KiB",
        "doesSheetExist": true
    },
    {
        "id": 0,
        "date": 1648336488,
        "uploadDate": 1648319097,
        "size": "8.3 KiB",
        "doesSheetExist": true
    }
]
```

### **GET** `/api/v1/logs/{log id}`

Example response:
```json
{
    "id": 0,
    "date": 1648336488,
    "uploadDate": 1648319097,
    "size": "8.3 KiB",
    "doesSheetExist": true
}
```
Errors:
* 400 Bad Request
  * The specified id is not an integer.
* 404 Not Found
  * No log could be found for the specified id.

## Deploying
Cabinet is designed to be deployed as a docker container. Pull the latest container by using:
```bash
docker pull noahhusby/cabinet:latest
```
Environment variables:
* `CABINET_DIR` - The persistent directory location for log storage.

## License

Distributed under the MIT License. See `LICENSE`
