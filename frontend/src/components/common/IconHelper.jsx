import React from 'react';
import * as Icons from 'lucide-react';

export default function IconHelper({ name, size = 18, className = '', color }) {
  if (!name) return <Icons.Circle size={size} className={className} color={color} />;

  // Normalize icon name
  const IconComponent = Icons[name] || Icons.Circle;
  return <IconComponent size={size} className={className} color={color} />;
}
