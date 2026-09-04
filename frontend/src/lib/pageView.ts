import { useEffect } from 'react';

/**
 * Logs a "user is viewing X" console entry whenever a page mounts or its
 * identifying id changes (e.g. navigating from one movie detail page to
 * another without unmounting). This is the "which page is the user on"
 * half of frontend activity logging — pair with the `[api]` logs in
 * lib/api.ts for what that page then requests from the backend.
 */
export function usePageView(pageName: string, id?: string | number): void {
  useEffect(() => {
    console.log(id === undefined ? `[page] viewing ${pageName}` : `[page] viewing ${pageName} (${id})`);
  }, [pageName, id]);
}
