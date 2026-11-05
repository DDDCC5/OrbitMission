import Foundation
import AVFoundation
import ImageIO
import UniformTypeIdentifiers

let asset=AVURLAsset(url: URL(fileURLWithPath:CommandLine.arguments[1]))
let generator=AVAssetImageGenerator(asset:asset)
generator.appliesPreferredTrackTransform=true
generator.requestedTimeToleranceBefore = .zero
generator.requestedTimeToleranceAfter = .zero
let directory=URL(fileURLWithPath:CommandLine.arguments[2])
try FileManager.default.createDirectory(at:directory,withIntermediateDirectories:true)
for seconds in [0.0,0.6,1.3,10.8,11.5,13.0] {
    let image=try generator.copyCGImage(at:CMTime(seconds:seconds,preferredTimescale:600),actualTime:nil)
    let path=directory.appendingPathComponent("frame-\(seconds).png")
    let output=CGImageDestinationCreateWithURL(path as CFURL,UTType.png.identifier as CFString,1,nil)!
    CGImageDestinationAddImage(output,image,nil)
    guard CGImageDestinationFinalize(output) else {fatalError("PNG write failed")}
    print(seconds,image.width,image.height,path.path)
}
print("duration",CMTimeGetSeconds(asset.duration))
