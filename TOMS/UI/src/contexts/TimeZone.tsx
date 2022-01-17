import { createContext, ReactNode, useContext, useState } from 'react';

import { TimeZone } from 'types/date';
import { Nullable } from 'types/util';

type ContextProps = {
  timeZone: TimeZone;
  setTimeZone(timeZone: TimeZone): void;
};

const TimeZoneContext = createContext<Nullable<ContextProps>>(null);

type Props = {
  children: ReactNode;
};

export const TimeZoneProvider = (props: Props) => {
  const [timeZone, setTimeZone] = useState(TimeZone.GMT);

  return (
    <TimeZoneContext.Provider value={{ timeZone, setTimeZone }}>
      {props.children}
    </TimeZoneContext.Provider>
  );
};

export const useTimeZoneContext = () => {
  const value = useContext(TimeZoneContext);

  if (!value) {
    throw new Error('useTimeZoneContext must be used within TimeZoneProvider');
  }

  return value;
};
