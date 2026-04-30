"use client"

import { useState, useRef, useCallback } from "react"
import { Camera, RotateCcw, ImagePlus, SwitchCamera, Zap, Check, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Header } from "@/components/skinai/header"
import { cn } from "@/lib/utils"
import { useRouter } from "next/navigation"

const captureGuides = [
  { icon: Zap, text: "밝은 조명" },
  { icon: Check, text: "정면 응시" },
  { icon: Check, text: "민낯 권장" },
]

export default function CapturePage() {
  const [capturedImage, setCapturedImage] = useState<string | null>(null)
  const [isCapturing, setIsCapturing] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const router = useRouter()

  const handleCapture = useCallback(() => {
    setIsCapturing(true)
    // Simulate capture with animation
    setTimeout(() => {
      // In a real app, this would use the camera API
      setCapturedImage("/placeholder-face.jpg")
      setIsCapturing(false)
    }, 500)
  }, [])

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (event) => {
        setCapturedImage(event.target?.result as string)
      }
      reader.readAsDataURL(file)
    }
  }, [])

  const handleRetake = useCallback(() => {
    setCapturedImage(null)
  }, [])

  const handleAnalyze = useCallback(() => {
    router.push("/loading")
  }, [router])

  const handleClose = useCallback(() => {
    router.push("/")
  }, [router])

  return (
    <div className="fixed inset-0 bg-foreground/95 flex flex-col">
      {/* Header */}
      <div className="relative z-10 flex items-center justify-between h-14 px-4 pt-[max(0px,env(safe-area-inset-top))]">
        <Button
          variant="ghost"
          size="icon"
          onClick={handleClose}
          className="rounded-full text-primary-foreground/80 hover:text-primary-foreground hover:bg-primary-foreground/10"
        >
          <X className="h-6 w-6" />
          <span className="sr-only">닫기</span>
        </Button>
        <h1 className="text-base font-semibold text-primary-foreground">피부 촬영</h1>
        <div className="w-10" />
      </div>

      {/* Camera View Area */}
      <div className="flex-1 flex items-center justify-center px-5">
        {capturedImage ? (
          <div className="relative w-full max-w-sm aspect-[3/4] rounded-3xl overflow-hidden">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={capturedImage}
              alt="촬영된 이미지"
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 border-4 border-primary/50 rounded-3xl" />
          </div>
        ) : (
          <div className="relative w-full max-w-sm aspect-[3/4] rounded-3xl border-2 border-dashed border-primary-foreground/30 flex items-center justify-center bg-primary-foreground/5">
            {/* Face Outline Guide */}
            <svg
              viewBox="0 0 200 250"
              className="w-48 h-60 text-primary-foreground/30"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <ellipse cx="100" cy="120" rx="70" ry="90" />
              <path d="M60 100 Q100 95 140 100" />
              <circle cx="70" cy="100" r="8" />
              <circle cx="130" cy="100" r="8" />
              <path d="M95 120 L95 140 L105 145" />
              <path d="M75 165 Q100 180 125 165" />
            </svg>
            
            {isCapturing && (
              <div className="absolute inset-0 bg-primary-foreground/20 flex items-center justify-center rounded-3xl backdrop-blur-sm">
                <div className="w-16 h-16 border-4 border-primary border-t-transparent rounded-full animate-spin" />
              </div>
            )}
            
            <p className="absolute bottom-6 left-0 right-0 text-sm text-primary-foreground/60 text-center">
              얼굴을 가이드 안에 맞춰주세요
            </p>
          </div>
        )}
      </div>

      {/* Bottom Controls */}
      <div className="px-5 py-6 pb-[max(24px,env(safe-area-inset-bottom))]">
        {/* Capture Guides */}
        {!capturedImage && (
          <div className="flex items-center justify-center gap-6 mb-6">
            {captureGuides.map((guide, index) => (
              <div
                key={index}
                className="flex items-center gap-1.5 text-xs text-primary-foreground/70"
              >
                <guide.icon className="h-3.5 w-3.5" />
                <span>{guide.text}</span>
              </div>
            ))}
          </div>
        )}
        
        {capturedImage ? (
          /* After Capture Controls */
          <div className="flex items-center gap-3 max-w-sm mx-auto">
            <Button
              variant="outline"
              size="lg"
              onClick={handleRetake}
              className="flex-1 h-14 rounded-2xl text-base bg-transparent border-primary-foreground/30 text-primary-foreground hover:bg-primary-foreground/10 hover:border-primary-foreground/50"
            >
              <RotateCcw className="mr-2 h-5 w-5" />
              다시 촬영
            </Button>
            <Button
              size="lg"
              onClick={handleAnalyze}
              className="flex-1 h-14 rounded-2xl text-base font-semibold shadow-lg shadow-primary/30"
            >
              분석하기
            </Button>
          </div>
        ) : (
          /* Before Capture Controls */
          <div className="flex items-center justify-center gap-6">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => fileInputRef.current?.click()}
              className="w-14 h-14 rounded-full bg-primary-foreground/10 hover:bg-primary-foreground/20 text-primary-foreground"
            >
              <ImagePlus className="h-6 w-6" />
              <span className="sr-only">갤러리에서 선택</span>
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileSelect}
              className="hidden"
            />
            
            <button
              onClick={handleCapture}
              disabled={isCapturing}
              className={cn(
                "w-20 h-20 rounded-full bg-primary flex items-center justify-center",
                "shadow-lg shadow-primary/40 transition-all duration-200",
                "hover:scale-105 active:scale-95",
                "focus:outline-none focus:ring-4 focus:ring-primary/30",
                isCapturing && "opacity-50 scale-95"
              )}
            >
              <div className="w-16 h-16 rounded-full border-4 border-primary-foreground/90 flex items-center justify-center">
                <Camera className="h-7 w-7 text-primary-foreground" />
              </div>
              <span className="sr-only">촬영하기</span>
            </button>
            
            <Button
              variant="ghost"
              size="icon"
              className="w-14 h-14 rounded-full bg-primary-foreground/10 hover:bg-primary-foreground/20 text-primary-foreground"
            >
              <SwitchCamera className="h-6 w-6" />
              <span className="sr-only">카메라 전환</span>
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
