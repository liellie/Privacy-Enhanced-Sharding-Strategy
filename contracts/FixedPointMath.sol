// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

library FixedPointMath {
    uint256 internal constant WAD = 1e18;

    /// ln(2) * 1e18
    int256 internal constant LN2 = 693147180559945309;
    /// e^{-1} * 1e18
    uint256 internal constant E_INV = 367879441171442322;

    // --------------------------------------------------------------------
     
    // --------------------------------------------------------------------

    function mulWad(uint256 a, uint256 b) internal pure returns (uint256) {
        return (a * b) / WAD;
    }

    function divWad(uint256 a, uint256 b) internal pure returns (uint256) {
        require(b != 0, "FPM: div by zero");
        return (a * WAD) / b;
    }

    function sqrt(uint256 x) internal pure returns (uint256 y) {
        if (x == 0) return 0;
        uint256 z = (x + 1) / 2;
        y = x;
        while (z < y) {
            y = z;
            z = (x / z + z) / 2;
        }
    }

    function sqrtWad(uint256 x) internal pure returns (uint256) {
        if (x == 0) return 0;
        require(x <= type(uint256).max / WAD, "FPM: sqrt overflow");
        return sqrt(x * WAD);
    }

    // --------------------------------------------------------------------
     
    // --------------------------------------------------------------------

    function lnWad(uint256 x) internal pure returns (int256) {
        require(x > 0, "FPM: ln of non-positive");
        if (x == WAD) return 0;

        int256 k = 0;
        uint256 m = x;

        while (m < WAD) {
            m <<= 1;
            k -= 1;
        }
        while (m >= 2 * WAD) {
            m >>= 1;
            k += 1;
        }

        uint256 z = ((m - WAD) * WAD) / (m + WAD);

        uint256 z2 = mulWad(z, z);
        uint256 term = z;
        uint256 sum = term;          // z
        term = mulWad(term, z2);
        sum += term / 3;             // z^3/3
        term = mulWad(term, z2);
        sum += term / 5;             // z^5/5
        term = mulWad(term, z2);
        sum += term / 7;             // z^7/7
        term = mulWad(term, z2);
        sum += term / 9;             // z^9/9
        term = mulWad(term, z2);
        sum += term / 11;            // z^11/11
        term = mulWad(term, z2);
        sum += term / 13;            // z^13/13
        term = mulWad(term, z2);
        sum += term / 15;            // z^15/15
        term = mulWad(term, z2);
        sum += term / 17;            // z^17/17
        term = mulWad(term, z2);
        sum += term / 19;            // z^19/19

        int256 lnM = int256(2 * sum);
        return lnM + k * LN2;
    }

    // --------------------------------------------------------------------
     
    // --------------------------------------------------------------------

    function expNegWad(uint256 x) internal pure returns (uint256) {
         
        if (x >= 42 * WAD) return 0;
        if (x == 0) return WAD;

        uint256 n = x / WAD;
        uint256 f = x % WAD;

        uint256 pos = WAD;           // 1
        uint256 neg = f;             // f
        uint256 term = mulWad(f, f);
        pos += term / 2;             // f^2/2!
        term = mulWad(term, f);
        neg += term / 6;             // f^3/3!
        term = mulWad(term, f);
        pos += term / 24;            // f^4/4!
        term = mulWad(term, f);
        neg += term / 120;           // f^5/5!
        term = mulWad(term, f);
        pos += term / 720;           // f^6/6!
        term = mulWad(term, f);
        neg += term / 5040;          // f^7/7!
        term = mulWad(term, f);
        pos += term / 40320;         // f^8/8!

        uint256 result = pos > neg ? pos - neg : 0;

        for (uint256 i = 0; i < n; i++) {
            result = mulWad(result, E_INV);
            if (result == 0) break;
        }
        return result;
    }
}
