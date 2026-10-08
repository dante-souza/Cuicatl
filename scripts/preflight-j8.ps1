# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'

$ExpectedSerial = '38c19745'
$ExpectedModel = 'SM-J810M'
$ExpectedProduct = 'j8y18lte'

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw 'adb was not found in PATH.'
}

$devices = @(adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S+\s+device$' })
if ($devices.Count -eq 0) {
    throw 'No authorized Android device is connected.'
}

$serials = @($devices | ForEach-Object { ($_ -split '\s+')[0] })
if ($serials -notcontains $ExpectedSerial) {
    throw "Expected J8 serial $ExpectedSerial is not connected. Connected: $($serials -join ', ')"
}

$model = (adb -s $ExpectedSerial shell getprop ro.product.model).Trim()
$product = (adb -s $ExpectedSerial shell getprop ro.product.device).Trim()

if ($model -ne $ExpectedModel) {
    throw "Refusing device action: expected model $ExpectedModel, got $model."
}

if ($product -ne $ExpectedProduct) {
    throw "Refusing device action: expected product $ExpectedProduct, got $product."
}

Write-Host "J8 preflight OK: $ExpectedSerial / $model / $product"
