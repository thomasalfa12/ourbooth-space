import { useUserContext } from "../context/UserContext";

export function useUserRole() {
  return useUserContext();
}