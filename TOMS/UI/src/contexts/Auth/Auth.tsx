import { createContext, ReactNode, useContext, useState } from 'react';

import { User } from 'types';
import { Nullable } from 'types/util';

import { mockUser } from './mockUser';

// Add very basic context for user, with mock
// Login and Logout functions will need adding in due course

type ContextProps = {
  user: User;
};

const AuthContext = createContext<Nullable<ContextProps>>(null);

type Props = {
  children: ReactNode;
};

export const AuthProvider = (props: Props) => {
  const { children } = props;
  const [user] = useState<User>(mockUser);

  return (
    <AuthContext.Provider value={{ user }}>{children}</AuthContext.Provider>
  );
};

export const useAuthContext = () => {
  const value = useContext(AuthContext);

  if (!value) {
    throw new Error('useAuthContext must be used within AuthProvider');
  }

  return value;
};
