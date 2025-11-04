/**
 * API Configuration
 * Switch between mock and real API
 */

// Set to true to use mock API (for testing without backend)
// Set to false to use real backend API
export const USE_MOCK_API = false;

// You can also check environment variable
// export const USE_MOCK_API = import.meta.env.VITE_USE_MOCK_API === 'true';

console.log(`[API CONFIG] Using ${USE_MOCK_API ? 'MOCK' : 'REAL'} API`);
