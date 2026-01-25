import { useControlMode, useSetControlMode } from "@/hooks/useControlMode"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Hand, Thermometer, Calendar } from "lucide-react"
import type { ControlMode } from "@/types"

interface ModeSelectorProps {
  disabled?: boolean
}

const MODE_CONFIG: { mode: ControlMode; label: string; icon: typeof Hand }[] = [
  { mode: "manual", label: "Manual", icon: Hand },
  { mode: "auto", label: "Auto", icon: Thermometer },
  { mode: "schedule", label: "Schedule", icon: Calendar },
]

/**
 * Mode selector component for switching between Manual, Auto, and Schedule modes
 * Displays as a segmented button group
 */
export function ModeSelector({ disabled = false }: ModeSelectorProps) {
  const { data: controlMode, isLoading } = useControlMode()
  const setControlMode = useSetControlMode()

  if (isLoading) {
    return <Skeleton className="h-10 w-72" />
  }

  const currentMode = controlMode?.mode ?? "manual"

  return (
    <div className="flex rounded-lg border border-input bg-background p-1" role="group" aria-label="Control mode selector">
      {MODE_CONFIG.map(({ mode, label, icon: Icon }) => {
        const isActive = currentMode === mode
        return (
          <Button
            key={mode}
            variant={isActive ? "default" : "ghost"}
            size="sm"
            onClick={() => setControlMode.mutate(mode)}
            disabled={disabled || setControlMode.isPending}
            className={`flex-1 gap-2 ${isActive ? "" : "text-muted-foreground"}`}
            aria-pressed={isActive}
          >
            <Icon className="h-4 w-4" />
            {label}
          </Button>
        )
      })}
    </div>
  )
}
