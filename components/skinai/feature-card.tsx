"use client"

import Link from "next/link"
import { cn } from "@/lib/utils"
import type { LucideIcon } from "lucide-react"

interface FeatureCardProps {
  href: string
  icon: LucideIcon
  iconColor?: string
  iconBg?: string
  title: string
  description: string
  className?: string
}

export function FeatureCard({
  href,
  icon: Icon,
  iconColor = "text-primary",
  iconBg = "bg-primary/10",
  title,
  description,
  className,
}: FeatureCardProps) {
  return (
    <Link
      href={href}
      className={cn(
        "block bg-card rounded-2xl p-5 shadow-sm border border-border",
        "transition-all duration-200",
        "hover:shadow-md hover:border-primary/20 active:scale-[0.98]",
        className
      )}
    >
      <div
        className={cn(
          "flex items-center justify-center w-12 h-12 rounded-xl mb-4",
          iconBg
        )}
      >
        <Icon className={cn("h-6 w-6", iconColor)} />
      </div>
      <h3 className="text-base font-semibold text-foreground mb-1">{title}</h3>
      <p className="text-sm text-muted-foreground leading-relaxed">{description}</p>
    </Link>
  )
}
