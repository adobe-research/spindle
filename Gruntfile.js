module.exports = function(grunt) {
  grunt.initConfig({
    buildcontrol: {
      options: {
        dir: 'demo',
        commit: true,
        push: true,
        message: 'Built %sourceName% from commit %sourceCommit% on branch %sourceBranch%.'
      },
      pages: {
        options: {
          remote: 'git@git.corp.adobe.com:amos/spindle.git',
          branch: 'gh-pages'
        }
      }
    }
  });
  grunt.loadNpmTasks('grunt-build-control');
  grunt.registerTask('deploy', ['buildcontrol:pages']);
}
