exports.defineAutoTests = function() {
  describe('cordova-plugin-bambuser', function() {

    it('is mounted', function() {
      expect(window.bambuser.broadcaster).toBeDefined();
      expect(window.bambuser.player).toBeDefined();
    });

    it('has broadcasting functions', function() {
      const { broadcaster } = window.bambuser;
      expect(broadcaster.hideViewfinder).toBeDefined();
      expect(broadcaster.setApplicationId).toBeDefined();
      expect(broadcaster.setCustomData).toBeDefined();
      expect(broadcaster.setSendPosition).toBeDefined();
      expect(broadcaster.setSaveOnServer).toBeDefined();
      expect(broadcaster.setTitle).toBeDefined();
      expect(broadcaster.setAuthor).toBeDefined();
      expect(broadcaster.showViewfinderBehindWebView).toBeDefined();
      expect(broadcaster.startBroadcast).toBeDefined();
      expect(broadcaster.stopBroadcast).toBeDefined();
      expect(broadcaster.switchCamera).toBeDefined();
    });

    it('has playback functions', function() {
      const { player } = window.bambuser;
      expect(player.close).toBeDefined();
      expect(player.hidePlayer).toBeDefined();
      expect(player.loadBroadcast).toBeDefined();
      expect(player.pause).toBeDefined();
      expect(player.resume).toBeDefined();
      expect(player.setApplicationId).toBeDefined();
      expect(player.setAudioVolume).toBeDefined();
      expect(player.showPlayerBehindWebView).toBeDefined();
    });

    it('should be able to show an idle viewfinder', function(done) {
      document.addEventListener('deviceready', onDeviceReady, false);
      function onDeviceReady() {
        const { broadcaster } = window.bambuser;
        broadcaster.setApplicationId('CORDOVA-PLUGIN-BAMBUSER-TESTSUITE', function() {
          broadcaster.showViewfinderBehindWebView(function() {
            done();
          }, function(err) { expect(false).toBe(true, err); done(); });
        }, function(err) { expect(false).toBe(true, err); done(); });
      }
    });

    it('should be able to show an idle player', function(done) {
      document.addEventListener('deviceready', onDeviceReady, false);
      function onDeviceReady() {
        const { player } = window.bambuser;
        player.setApplicationId('CORDOVA-PLUGIN-BAMBUSER-TESTSUITE', function() {
          player.showPlayerBehindWebView(function() {
            done();
          }, function(err) { expect(false).toBe(true, err); done(); });
        }, function(err) { expect(false).toBe(true, err); done(); });
      }
    });

  });
};
