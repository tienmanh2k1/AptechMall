/**
 * Manual test file for marketplace utilities
 * Run this in the browser console to verify functionality
 */

import {
  parseGlobalId,
  buildGlobalId,
  legacyToCanonical,
  MARKETPLACE
} from './marketplace.js';

// Test parseGlobalId
console.group('Testing parseGlobalId');

// Valid cases
console.log('Valid AliExpress:', parseGlobalId('ae:1005005244562338'));
// Expected: { marketplace: 'aliexpress', productId: '1005005244562338', isValid: true }

console.log('Valid Taobao:', parseGlobalId('tb:6543210987'));
// Expected: { marketplace: 'taobao', productId: '6543210987', isValid: true }

console.log('Valid 1688:', parseGlobalId('a1688:898144857257'));
// Expected: { marketplace: '1688', productId: '898144857257', isValid: true }

// Invalid cases
console.log('Legacy format:', parseGlobalId('1005005244562338'));
// Expected: { isLegacy: true, isValid: false }

console.log('Invalid prefix:', parseGlobalId('xx:1234'));
// Expected: { isValid: false, error: 'VALIDATION_ERROR...' }

console.log('Empty productId:', parseGlobalId('ae:'));
// Expected: { isValid: false, error: 'VALIDATION_ERROR...' }

console.log('Malformed:', parseGlobalId('ae:123:456'));
// Expected: { isValid: false, error: 'VALIDATION_ERROR...' }

console.groupEnd();

// Test buildGlobalId
console.group('Testing buildGlobalId');

console.log('Build AliExpress:', buildGlobalId(MARKETPLACE.ALIEXPRESS, '1005005244562338'));
// Expected: 'ae:1005005244562338'

console.log('Build Taobao:', buildGlobalId(MARKETPLACE.TAOBAO, '6543210987'));
// Expected: 'tb:6543210987'

console.log('Build 1688:', buildGlobalId(MARKETPLACE.ALIBABA_1688, '898144857257'));
// Expected: 'a1688:898144857257'

try {
  console.log('Invalid marketplace:', buildGlobalId('invalid', '123'));
} catch (error) {
  console.log('Caught error:', error.message);
  // Expected: Error thrown
}

console.groupEnd();

// Test legacyToCanonical
console.group('Testing legacyToCanonical');

console.log('Legacy to canonical:', legacyToCanonical('1005005244562338'));
// Expected: 'ae:1005005244562338'

console.groupEnd();

console.log('✅ All tests logged. Check results above.');
