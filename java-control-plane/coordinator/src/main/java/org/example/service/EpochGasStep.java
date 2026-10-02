package org.example.service;

import java.math.BigInteger;

public record EpochGasStep(String name, BigInteger gasUsed, long wallMs) {
}
