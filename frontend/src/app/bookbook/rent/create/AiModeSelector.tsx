import React from 'react';

interface AiModeSelectorProps {
    isAiModeEnabled: boolean;
    onToggle: (enabled: boolean) => void;
}

export default function AiModeSelector({ isAiModeEnabled, onToggle }: AiModeSelectorProps) {
    return (
        <div className="mb-6">
            <div className={`inline-block px-4 py-2 rounded-lg border-2 transition-all duration-300 ${
                isAiModeEnabled 
                    ? 'bg-[#D5BAA3] border-[#C2A794]' 
                    : 'bg-gray-100 border-gray-300'
            }`}>
                <div className="flex items-center space-x-4">
                    <div className="flex items-center space-x-3">
                        <span className={`font-medium transition-colors duration-300 ${
                            isAiModeEnabled ? 'text-white' : 'text-gray-700'
                        }`}>
                            AI로 작성하기
                        </span>
                    </div>
                    <button
                        type="button"
                        onClick={() => onToggle(!isAiModeEnabled)}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                            isAiModeEnabled 
                                ? 'bg-white focus:ring-white' 
                                : 'bg-gray-400 focus:ring-gray-400'
                        }`}
                    >
                        <span
                            className={`inline-block h-4 w-4 transform rounded-full transition-all duration-300 ${
                                isAiModeEnabled 
                                    ? 'translate-x-6 bg-[#D5BAA3]' 
                                    : 'translate-x-1 bg-white'
                            }`}
                        />
                    </button>
                </div>
            </div>
        </div>
    );
}
