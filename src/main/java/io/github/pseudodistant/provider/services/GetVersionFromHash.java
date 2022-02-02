package io.github.pseudodistant.provider.services;

import net.fabricmc.loader.impl.util.version.StringVersion;

public class GetVersionFromHash {
    public static StringVersion getVersionFromHash(String hash) {
        String gameVersion = switch(hash) {
            case "f211b7cbfb31584af3cdce35f874fe0a" -> "1.9.1";
            case "999940dbb17877e6bc6231476494dc26" -> "1.9.2";
            case "53e7b293fdc5cd77340fea4373b8faaa" -> "1.9.3";
            case "8f9f93761df1fb7caca44a79653f0f1a" -> "1.9.4";
            case "f756156871a2d0cb615936a3eb5c7b93" -> "2.0.0";
            case "3a324f65eaf17704030a009976cb9201" -> "2.0.1";
            case "b5f4ebec06729f662321f53e0954c9e8" -> "2.0.2";
            case "ba70baf0f36e06339709a49d09a56d86" -> "2.0.3";
            case "b8a93275922008cb526c305b3854c432" -> "2.0.4";
            case "e16893e756ef1b63c8b8dc98d3e1c77d" -> "2.0.5";
            case "b49c32739d7266f8147e47ea09754f8d" -> "2.0.6";
            case "5b283ce4e1f0bc41205c11eedf5610d0" -> "2.0.7";
            case "e96d6ca22a402d69b8e35e4a80cd2582" -> "2.1.0-dev1";
            default -> "0.0.0";
        };
        return new StringVersion(gameVersion);
    }
}
