import { useState } from "react"
import { useCreateOverride } from "@/hooks/useOverride"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Select } from "@/components/ui/select"
import { X, Clock } from "lucide-react"
import type { OverrideDuration } from "@/types"

interface OverrideDialogProps {
  isOpen: boolean
  onClose: () => void
  onSuccess?: () => void
}

const DURATION_OPTIONS: { value: OverrideDuration; label: string; description: string }[] = [
  { value: "1h", label: "1 hour", description: "Override for 1 hour" },
  { value: "2h", label: "2 hours", description: "Override for 2 hours" },
  { value: "4h", label: "4 hours", description: "Override for 4 hours" },
  { value: "until_next", label: "Until next schedule", description: "Override until the next scheduled period begins" },
]

/**
 * Dialog component for creating a new override
 * Shown when user makes manual changes during an active schedule
 */
export function OverrideDialog({ isOpen, onClose, onSuccess }: OverrideDialogProps) {
  const [duration, setDuration] = useState<OverrideDuration>("1h")
  const createOverride = useCreateOverride()

  if (!isOpen) {
    return null
  }

  const handleConfirm = async () => {
    try {
      await createOverride.mutateAsync({ duration })
      onSuccess?.()
      onClose()
    } catch (error) {
      // Error is handled by React Query
      console.error("Failed to create override:", error)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            Hold Duration
          </CardTitle>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            How long would you like to hold these settings? The schedule will resume after this time.
          </p>

          <div className="space-y-2">
            <label className="text-sm font-medium">Duration</label>
            <Select
              value={duration}
              onValueChange={(value) => setDuration(value as OverrideDuration)}
            >
              {DURATION_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
            <p className="text-xs text-muted-foreground">
              {DURATION_OPTIONS.find((o) => o.value === duration)?.description}
            </p>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button onClick={handleConfirm} disabled={createOverride.isPending}>
              {createOverride.isPending ? "Creating..." : "Confirm"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
