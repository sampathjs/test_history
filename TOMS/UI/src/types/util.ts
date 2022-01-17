type Join<Left, Right> = Left extends string | number
  ? Right extends string | number
    ? '' extends Left
      ? Right
      : `${Left}.${Right}`
    : never
  : never;

type PathValues<Type, Prefix = ''> = {
  [Key in keyof Type]: Type[Key] extends
    | Record<string, unknown>
    | Array<unknown>
    ? [Join<Prefix, Key>, Type[Key]] | PathValues<Type[Key], Join<Prefix, Key>>
    : [Join<Prefix, Key>, Type[Key]];
}[keyof Type];

export type Paths<
  Type,
  TypePathValues extends [unknown, unknown] = PathValues<Type>
> = TypePathValues[0];

export type PathValue<
  Path extends TypePathValues[0],
  Type,
  TypePathValues extends [unknown, unknown] = PathValues<Type>
> = TypePathValues extends [Path, unknown] ? TypePathValues[1] : never;

export type Nullable<Type> = Type | null;
