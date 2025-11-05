/**
 * TranslationToggle Component
 * Toggle button to switch between original and translated content
 */

import React from 'react';
import { Languages, Loader } from 'lucide-react';

const TranslationToggle = ({
  showOriginal,
  onToggle,
  isTranslating,
  hasTranslation,
  sourceLang = 'EN',
  targetLang = 'VI'
}) => {
  // Don't show toggle if no translation available and not currently translating
  if (!hasTranslation && !isTranslating) {
    return null;
  }

  return (
    <div className="flex items-center gap-3 bg-white rounded-lg p-3 shadow-sm border border-gray-200">
      {/* Translation Status Icon */}
      <div className="flex-shrink-0">
        {isTranslating ? (
          <Loader className="w-5 h-5 text-blue-600 animate-spin" />
        ) : (
          <Languages className="w-5 h-5 text-blue-600" />
        )}
      </div>

      {/* Translation Status Text */}
      <div className="flex-1 min-w-0">
        {isTranslating ? (
          <p className="text-sm text-gray-700 font-medium">
            Đang dịch sang tiếng Việt...
          </p>
        ) : (
          <p className="text-sm text-gray-600">
            {showOriginal ? (
              <span>
                Đang xem bản gốc (<span className="font-semibold">{sourceLang}</span>)
              </span>
            ) : (
              <span>
                Đang xem bản dịch (<span className="font-semibold">{targetLang}</span>)
              </span>
            )}
          </p>
        )}
      </div>

      {/* Toggle Button */}
      {!isTranslating && hasTranslation && (
        <button
          onClick={onToggle}
          className="flex-shrink-0 px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 rounded-lg transition-colors border border-blue-200 hover:border-blue-300"
        >
          {showOriginal ? 'Xem bản dịch' : 'Xem bản gốc'}
        </button>
      )}
    </div>
  );
};

export default TranslationToggle;
