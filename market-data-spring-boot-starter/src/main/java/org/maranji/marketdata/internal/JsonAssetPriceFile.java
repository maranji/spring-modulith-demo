package org.maranji.marketdata.internal;

import java.util.List;

record JsonAssetPriceFile(String asset, String currency, List<JsonPricePoint> prices) {
}
