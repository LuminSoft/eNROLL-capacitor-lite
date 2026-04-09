require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'EnrollCapacitorNeo'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = package['author']
  s.source = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.source_files = 'ios/Sources/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.vendored_frameworks = 'ios/Frameworks/EnrollFramework.xcframework'
  s.ios.deployment_target = '15.5'
  s.dependency 'Capacitor'
  s.dependency 'EnrollNeoCore', '1.0.6'
  s.dependency 'FirebaseRemoteConfig'
  s.swift_version = '5.0'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386'
  }
end
