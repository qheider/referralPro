export function extractApiErrorMessage(error: unknown, fallbackMessage: string): string {
  if (!error || typeof error !== 'object') {
    return fallbackMessage;
  }

  const candidate = error as {
    message?: string;
    error?: {
      message?: string;
    };
  };

  return candidate.error?.message || candidate.message || fallbackMessage;
}
